# Decryptor test support

These sources are added only to the test source roots of decryptor, config, and
common by build-helper-maven-plugin. Each module compiles its own test output;
there is no test artifact dependency or attached test JAR to install or deploy.
The fixture provider's public password is only for synthetic CRYPT values.
ForkedStartup runs startup probes in separate JVMs to isolate static initialization
and configuration properties from the test runner.
