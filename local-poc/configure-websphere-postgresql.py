"""Configure the local PostgreSQL connection in WebSphere.

This is Jython executed by WebSphere's wsadmin tool. AdminConfig and AdminTask
are WebSphere-provided global objects, so they are intentionally not imported.
The script is safe to run repeatedly: existing objects are updated and missing
objects are created.
"""

import os


# WebSphere location in which the JDBC objects will be created.
CELL_NAME = "DefaultCell01"
NODE_NAME = "DefaultNode01"
SERVER_NAME = "server1"

# Names shared by WebSphere configuration and the deployed application.
JDBC_PROVIDER_NAME = "Legacy POC PostgreSQL JDBC Provider"
DATA_SOURCE_NAME = "LegacyPocDS"
JNDI_NAME = "jdbc/LegacyPocDS"
AUTH_ALIAS_NAME = "LegacyPocPostgresAuth"

# Local-container PostgreSQL settings. The password is never stored here.
DATABASE_USER = "legacy_poc"
DATABASE_HOST = "legacy-poc-postgres"
DATABASE_PORT = "5432"
DATABASE_NAME = "legacy_poc"
JDBC_DRIVER_PATH = "/work/lib/postgresql.jar"


def required_environment_value(name):
    """Return a required environment variable without printing its value."""
    value = os.environ.get(name)
    if not value:
        raise Exception(name + " must be supplied to wsadmin")
    return value


def find_named_object(config_type, object_name, parent_id=None):
    """Find one WebSphere configuration object by its name attribute."""
    if parent_id:
        configured_objects = AdminConfig.list(config_type, parent_id)
    else:
        configured_objects = AdminConfig.list(config_type)

    for object_id in configured_objects.splitlines():
        if object_id and AdminConfig.showAttribute(object_id, "name") == object_name:
            return object_id
    return None


def find_authentication_alias(alias_name):
    """Find an alias whether WebSphere stores it as alias or cell/alias."""
    for object_id in AdminConfig.list("JAASAuthData").splitlines():
        if object_id:
            configured_alias = AdminConfig.showAttribute(object_id, "alias")
            if configured_alias == alias_name or configured_alias.endswith("/" + alias_name):
                return object_id
    return None


def configure_authentication_alias(database_password):
    """Create or update the secured username/password entry."""
    print "Step 1/4: Configuring the J2C authentication alias"
    auth_id = find_authentication_alias(AUTH_ALIAS_NAME)

    if auth_id:
        AdminConfig.modify(auth_id, [
            ["userId", DATABASE_USER],
            ["password", database_password]
        ])
    else:
        AdminTask.createAuthDataEntry([
            "-alias", AUTH_ALIAS_NAME,
            "-user", DATABASE_USER,
            "-password", database_password,
            "-description", "Local PostgreSQL credentials for Legacy POC"
        ])
        auth_id = find_authentication_alias(AUTH_ALIAS_NAME)

    if not auth_id:
        raise Exception("WebSphere did not create authentication alias " + AUTH_ALIAS_NAME)

    # WebSphere may qualify the value with the cell name. The DataSource must
    # use the exact alias value stored by WebSphere.
    return AdminConfig.showAttribute(auth_id, "alias")


def get_target_server():
    """Resolve the server scope where the provider and DataSource belong."""
    server_path = "/Cell:%s/Node:%s/Server:%s/" % (
        CELL_NAME, NODE_NAME, SERVER_NAME
    )
    server_id = AdminConfig.getid(server_path)
    if not server_id:
        raise Exception("WebSphere server scope was not found: " + server_path)
    return server_id


def configure_jdbc_provider(server_id):
    """Create or update the server-scoped PostgreSQL JDBC provider."""
    print "Step 2/4: Configuring the PostgreSQL JDBC provider"
    provider_id = find_named_object("JDBCProvider", JDBC_PROVIDER_NAME, server_id)

    common_attributes = [
        ["description", "PostgreSQL JDBC provider for the local Legacy POC"],
        ["implementationClassName", "org.postgresql.ds.PGConnectionPoolDataSource"],
        ["classpath", JDBC_DRIVER_PATH],
        ["nativepath", ""]
    ]

    if provider_id:
        AdminConfig.modify(provider_id, common_attributes)
    else:
        provider_id = AdminConfig.create("JDBCProvider", server_id, [
            ["name", JDBC_PROVIDER_NAME],
            ["providerType", "User-defined JDBC Provider"],
            ["xa", "false"]
        ] + common_attributes)

    return provider_id


def configure_data_source(provider_id, auth_alias_value):
    """Create or update the DataSource that Spring looks up through JNDI."""
    print "Step 3/4: Configuring DataSource " + JNDI_NAME
    data_source_id = find_named_object("DataSource", DATA_SOURCE_NAME, provider_id)
    attributes = [
        ["name", DATA_SOURCE_NAME],
        ["description", "PostgreSQL DataSource for Legacy POC"],
        ["jndiName", JNDI_NAME],
        ["authDataAlias", auth_alias_value],
        ["authMechanismPreference", "BASIC_PASSWORD"],
        ["datasourceHelperClassname", "com.ibm.websphere.rsadapter.GenericDataStoreHelper"],
        ["statementCacheSize", "10"]
    ]

    if data_source_id:
        AdminConfig.modify(data_source_id, attributes)
    else:
        data_source_id = AdminConfig.create("DataSource", provider_id, attributes)

    return data_source_id


def set_data_source_property(property_set_id, property_name, property_type, property_value):
    """Create or update one PostgreSQL DataSource custom property."""
    property_id = find_named_object(
        "J2EEResourceProperty", property_name, property_set_id
    )
    attributes = [
        ["name", property_name],
        ["type", property_type],
        ["value", property_value],
        ["required", "true"]
    ]

    if property_id:
        AdminConfig.modify(property_id, attributes)
    else:
        AdminConfig.create("J2EEResourceProperty", property_set_id, attributes)


def configure_connection_properties(data_source_id):
    """Set the host, port, and database passed to the PostgreSQL driver."""
    print "Step 4/4: Configuring PostgreSQL connection properties"
    property_set_id = AdminConfig.showAttribute(data_source_id, "propertySet")
    if not property_set_id:
        property_set_id = AdminConfig.create(
            "J2EEResourcePropertySet", data_source_id, []
        )

    set_data_source_property(
        property_set_id, "serverName", "java.lang.String", DATABASE_HOST
    )
    set_data_source_property(
        property_set_id, "portNumber", "java.lang.Integer", DATABASE_PORT
    )
    set_data_source_property(
        property_set_id, "databaseName", "java.lang.String", DATABASE_NAME
    )


def main():
    database_password = required_environment_value("DB_PASSWORD")
    auth_alias_value = configure_authentication_alias(database_password)
    server_id = get_target_server()
    provider_id = configure_jdbc_provider(server_id)
    data_source_id = configure_data_source(provider_id, auth_alias_value)
    configure_connection_properties(data_source_id)

    # Changes are not persistent until AdminConfig.save() is called.
    AdminConfig.save()
    print "Configuration saved successfully"
    print "  JDBC provider: " + JDBC_PROVIDER_NAME
    print "  DataSource JNDI: " + JNDI_NAME
    print "  Authentication alias: " + auth_alias_value


main()
