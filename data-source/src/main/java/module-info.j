module com.networknt.data.source {
    exports com.networknt.db;

    requires com.networknt.config;

    requires com.zaxxer.hikari;
    requires java.sql;
}