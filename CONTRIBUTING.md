# How to contribute

Third-party patches are essential for keeping Light 4J great. We simply can't
access the huge number of platforms and myriad configurations for running
Light 4J. We want to keep it as easy as possible to contribute changes that
get things working in your environment. There are a few guidelines that we
need contributors to follow so that we can have a chance of keeping on
top of things.

## Core vs Modules

New functionality is typically directed toward middleware components to provide 
a slimmer Framework Core, reducing its surface area, and to allow greater freedom 
for middleware developers and maintainers. 
 
If you are unsure of whether your contribution should be implemented as a
middleware component or part of Framework Core, you may visit
[Gitter Chat](https://gitter.im/networknt/light-4j) for advice.

## Getting Started

* Make sure you have a [GitHub account](https://github.com/signup/free)
* Submit a ticket for your issue, assuming one does not already exist.
  * Clearly describe the issue including steps to reproduce when it is a bug.
  * Make sure you fill in the earliest version that you know has the issue.
* Fork the repository on GitHub

## Making Changes

* Create a topic branch from where you want to base your work.
  * This is usually the develop branch.
  * Only target master branch if you are certain your fix must be on that
    branch.
  * To quickly create a topic branch based on develop; `git checkout -b
    fix32 develop`. Please avoid working directly on the `master` branch.
* Make commits of logical units.
* Check for unnecessary whitespace with `git diff --check` before committing.
* Make sure your commit messages are in the proper format.
* Make sure you have added the necessary tests for your changes.
* Run all the tests to assure nothing else was accidentally broken.

## Making Trivial Changes

### Documentation

For changes of a trivial nature to comments and documentation, it is not
necessary to create a new issue. 

### Example

Adding new examples or enhance examples won't need to create a new issue.


## Submitting Changes

* Push your changes to a topic branch in your fork of the repository.
* Submit a pull request to the repository in the networknt organization.
* The core team looks at Pull Requests on a regular basis and will merge the pull request. 
* Sometimes, we might need to contact contributor understand the details of the pull request.

# Additional Resources

* [General GitHub documentation](https://help.github.com/)
* [GitHub pull request documentation](https://help.github.com/send-pull-requests/)
* [Light 4J Document](https://networknt.github.io/light-4j/)
* [Light 4J Gitter](https://gitter.im/networknt/light-4j)