module com.networknt.email.sender {
    exports com.networknt.email;

    requires com.networknt.common;
    requires com.networknt.config;
    requires com.networknt.utility;
    
    requires java.mail;
    requires activation;
    requires org.slf4j;
}