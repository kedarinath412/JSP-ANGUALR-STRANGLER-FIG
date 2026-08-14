import os

CELL = "DefaultCell01"
NODE = "DefaultNode01"
SERVER = "server1"
PROVIDER_NAME = "Legacy POC PostgreSQL JDBC Provider"
DATA_SOURCE_NAME = "LegacyPocDS"
JNDI_NAME = "jdbc/LegacyPocDS"
AUTH_ALIAS = "LegacyPocPostgresAuth"
JDBC_JAR = "/work/lib/postgresql.jar"

db_password = os.environ.get("DB_PASSWORD")
if not db_password:
    raise Exception("DB_PASSWORD must be supplied to wsadmin")


def find_by_name(config_type, name):
    for item in AdminConfig.list(config_type).splitlines():
        if item and AdminConfig.showAttribute(item, "name") == name:
            return item
    return None


def find_auth_alias(alias):
    for item in AdminConfig.list("JAASAuthData").splitlines():
        if not item:
            continue
        configured = AdminConfig.showAttribute(item, "alias")
        if configured == alias or configured.endswith("/" + alias):
            return item
    return None


auth = find_auth_alias(AUTH_ALIAS)
if auth:
    AdminConfig.modify(auth, [["userId", "legacy_poc"], ["password", db_password]])
else:
    AdminTask.createAuthDataEntry([
        "-alias", AUTH_ALIAS,
        "-user", "legacy_poc",
        "-password", db_password,
        "-description", "Local PostgreSQL credentials for Legacy POC"
    ])
    auth = find_auth_alias(AUTH_ALIAS)

auth_alias_value = AdminConfig.showAttribute(auth, "alias")
server = AdminConfig.getid("/Cell:%s/Node:%s/Server:%s/" % (CELL, NODE, SERVER))

provider = find_by_name("JDBCProvider", PROVIDER_NAME)
provider_attributes = [
    ["name", PROVIDER_NAME],
    ["description", "PostgreSQL JDBC provider for the local Legacy POC"],
    ["implementationClassName", "org.postgresql.ds.PGConnectionPoolDataSource"],
    ["classpath", JDBC_JAR],
    ["nativepath", ""],
    ["providerType", "User-defined JDBC Provider"],
    ["xa", "false"]
]
if provider:
    AdminConfig.modify(provider, [
        ["description", "PostgreSQL JDBC provider for the local Legacy POC"],
        ["implementationClassName", "org.postgresql.ds.PGConnectionPoolDataSource"],
        ["classpath", JDBC_JAR],
        ["nativepath", ""]
    ])
else:
    provider = AdminConfig.create("JDBCProvider", server, provider_attributes)

data_source = find_by_name("DataSource", DATA_SOURCE_NAME)
data_source_attributes = [
    ["name", DATA_SOURCE_NAME],
    ["description", "PostgreSQL DataSource for Legacy POC"],
    ["jndiName", JNDI_NAME],
    ["authDataAlias", auth_alias_value],
    ["authMechanismPreference", "BASIC_PASSWORD"],
    ["datasourceHelperClassname", "com.ibm.websphere.rsadapter.GenericDataStoreHelper"],
    ["statementCacheSize", "10"]
]
if data_source:
    AdminConfig.modify(data_source, data_source_attributes)
else:
    data_source = AdminConfig.create("DataSource", provider, data_source_attributes)

property_set = AdminConfig.showAttribute(data_source, "propertySet")
if not property_set:
    property_set = AdminConfig.create("J2EEResourcePropertySet", data_source, [])

desired_properties = {
    "serverName": ["java.lang.String", "legacy-poc-postgres"],
    "portNumber": ["java.lang.Integer", "5432"],
    "databaseName": ["java.lang.String", "legacy_poc"]
}

existing = {}
for item in AdminConfig.list("J2EEResourceProperty", property_set).splitlines():
    if item:
        existing[AdminConfig.showAttribute(item, "name")] = item

for property_name in desired_properties.keys():
    property_type, property_value = desired_properties[property_name]
    attributes = [
        ["name", property_name],
        ["type", property_type],
        ["value", property_value],
        ["required", "true"]
    ]
    if property_name in existing:
        AdminConfig.modify(existing[property_name], attributes)
    else:
        AdminConfig.create("J2EEResourceProperty", property_set, attributes)

AdminConfig.save()
print "Configured JDBC provider: " + PROVIDER_NAME
print "Configured DataSource: " + JNDI_NAME
print "Configured authentication alias: " + auth_alias_value
