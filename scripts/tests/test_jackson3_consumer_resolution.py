from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch, Mock

sys.path.insert(0, str(Path(__file__).parents[1]))
import resolve_jackson3_consumer_sources as consumer
import prepare_jackson3_consumer_promotion as promotion

CORE = "a" * 40
POLICY = "b" * 40


class ConsumerResolverTest(unittest.TestCase):
    def test_core_cannot_be_a_moving_branch(self):
        with self.assertRaisesRegex(ValueError, "explicit full"):
            consumer.validate_inputs("fix-jackson3-mapper-policy", {}, {})
        consumer.validate_inputs(CORE, {"light-rest-4j": None}, {})

    def test_unknown_overrides_and_reserved_or_invalid_plans_are_rejected(self):
        for plan, overrides in (({}, {"other": CORE}), ({"light-4j": None}, {}),
                                ({"repo": "; shell"}, {}), ({"repo": None}, {"repo": "master"})):
            with self.subTest(plan=plan, overrides=overrides), self.assertRaises(ValueError):
                consumer.validate_inputs(CORE, plan, overrides)

    def test_explicit_remaining_ref_never_resolves_a_branch(self):
        github = Mock(side_effect=AssertionError("Unexpected API call"))
        self.assertEqual(CORE, consumer.resolve_remaining("repo", "migration", CORE, github))

    def test_remaining_default_and_merged_branches_use_default_head(self):
        for branch in (None, "migration"):
            replies = ([[], [{"state": "closed", "merged_at": "date"}]],) if branch else ()
            github = Mock(side_effect=list(replies) + [{"default_branch": "main"}, {"sha": CORE}])
            self.assertEqual(CORE, consumer.resolve_remaining("repo", branch, None, github))

    def test_open_remaining_pr_uses_paginated_head_and_closed_unmerged_fails(self):
        github = Mock(return_value=[[], [{"state": "open", "head": {"sha": CORE}}]])
        self.assertEqual(CORE, consumer.resolve_remaining("repo", "migration", None, github))
        self.assertTrue(github.call_args.args[1])
        github = Mock(return_value=[[{"state": "closed", "merged_at": None}]])
        with self.assertRaisesRegex(ValueError, "closed without merging"):
            consumer.resolve_remaining("repo", "migration", None, github)

    def test_ambiguous_remaining_prs_fail(self):
        github = Mock(return_value=[[{"state": "open"}, {"state": "open"}]])
        with self.assertRaisesRegex(ValueError, "Ambiguous"):
            consumer.resolve_remaining("repo", "migration", None, github)

    def test_core_evidence_is_selected_before_remaining_repositories(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "outputs"
            argv = ["resolve", "--core-sha", CORE, "--repositories", '{"repo":null}',
                    "--source-refs", "{}", "--report", str(Path(directory) / "report"), "--github-output", str(output)]
            calls = []
            def selected(*args, **kwargs):
                calls.append("select core")
                self.assertIn(CORE, args[0])
            def remaining(*args):
                calls.append("remaining")
                return POLICY
            with patch.object(sys, "argv", argv), patch.object(consumer.subprocess, "run", side_effect=selected), \
                    patch.object(consumer, "resolve_remaining", side_effect=remaining):
                consumer.main()
            self.assertEqual(["select core", "remaining"], calls)
            self.assertIn("light_4j=" + CORE, output.read_text())


LEGACY = '''env:
  # Proposed immutable policy pin; promotion requires review of this commit.
  JACKSON3_QUALIFIER_REVISION: ''' + "c" * 40 + '''
jobs:
  test:
    steps:
      - name: Resolve prerequisite commits
        run: |
          resolve() {
            :
          }
          resolve light-4j fix-jackson3-mapper-policy
          resolve json-overlay
          resolve light-aws-lambda migrate-jackson-3
      - uses: actions/checkout@v4
        with:
          repository: networknt/light-4j
      - uses: actions/checkout@v4
        with:
          repository: networknt/yaml-rule
      - uses: actions/checkout@v4
        with:
          repository: networknt/http-client
      - uses: actions/checkout@v4
        with:
          repository: networknt/json-overlay
      - uses: actions/checkout@v4
        with:
          repository: networknt/light-aws-lambda
'''


class ConsumerPromotionTest(unittest.TestCase):
    def test_nested_legacy_error_group_is_not_mistaken_for_the_function_end(self):
        nested = LEGACY.replace("            :\n", "            false || {\n              :\n            }\n")
        text = promotion.promoted_workflow(nested, POLICY, CORE)
        self.assertIn('"json-overlay":null', text)

    def test_call_indentation_tabs_quotes_and_comments_preserve_every_prerequisite(self):
        for call in ("            resolve json-overlay", "          resolve\tjson-overlay",
                     "          'resolve' \"json-overlay\" # formatting change"):
            with self.subTest(call=call):
                text = promotion.promoted_workflow(LEGACY.replace("          resolve json-overlay", call), POLICY, CORE)
                self.assertIn('"json-overlay":null', text)
                self.assertIn('"light-aws-lambda":"migrate-jackson-3"', text)

    def test_unsupported_command_forms_fail_instead_of_silently_dropping_a_repository(self):
        for call in ("command resolve json-overlay", "resolve json-overlay; resolve extra",
                     'resolve "$NEXT_REPOSITORY"', "if true; then resolve json-overlay; fi",
                     "resolve " + chr(92) + "\n            json-overlay"):
            with self.subTest(call=call), self.assertRaises(ValueError):
                promotion.promoted_workflow(LEGACY.replace("resolve json-overlay", call), POLICY, CORE)

    def test_missing_call_but_retained_checkout_is_rejected_independently(self):
        with self.assertRaisesRegex(ValueError, "Resolver/checkouts differ"):
            promotion.promoted_workflow(LEGACY.replace("          resolve json-overlay\n", ""), POLICY, CORE)

    def test_duplicate_calls_and_dynamic_checkout_inventory_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            promotion.promoted_workflow(LEGACY.replace("resolve json-overlay", "resolve json-overlay\n          resolve json-overlay"), POLICY, CORE)
        with self.assertRaisesRegex(ValueError, "literal checkout"):
            promotion.promoted_workflow(LEGACY.replace("networknt/json-overlay", "${{ matrix.repository }}"), POLICY, CORE)

    def test_prepared_followup_has_literal_pins_and_no_copied_shell_resolver(self):
        text = promotion.promoted_workflow(LEGACY, POLICY, CORE)
        self.assertIn("JACKSON3_QUALIFIER_REVISION: " + POLICY, text)
        self.assertIn("JACKSON3_CORE_REVISION: " + CORE, text)
        self.assertIn("resolve_jackson3_consumer_sources.py", text)
        self.assertNotIn("resolve()", text)
        self.assertNotIn("fix-jackson3-mapper-policy", text)
        self.assertIn('"light-aws-lambda":"migrate-jackson-3"', text)

    def test_promotion_rejects_branch_names_and_open_branch_commits(self):
        with self.assertRaises(ValueError):
            promotion.promoted_workflow(LEGACY, "master", CORE)
        with patch.object(promotion.subprocess, "run", return_value=Mock(returncode=1)), \
                self.assertRaisesRegex(ValueError, "merge/review first"):
            promotion.require_on_master(Path("repo"), POLICY)

    def test_already_promoted_or_missing_core_resolver_is_not_silently_rewritten(self):
        with self.assertRaises(ValueError):
            promotion.promoted_workflow(LEGACY.replace("resolve light-4j", "resolve other"), POLICY, CORE)
