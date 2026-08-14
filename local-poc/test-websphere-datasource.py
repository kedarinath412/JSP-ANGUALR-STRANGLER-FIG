"""Ask WebSphere to test the configured LegacyPocDS connection."""

CELL_NAME = "DefaultCell01"
NODE_NAME = "DefaultNode01"
SERVER_NAME = "server1"
JDBC_PROVIDER_NAME = "Legacy POC PostgreSQL JDBC Provider"
DATA_SOURCE_NAME = "LegacyPocDS"


def data_source_configuration_path():
    """Build the exact WebSphere configuration path for this DataSource."""
    return (
        "/Cell:%s/Node:%s/Server:%s/JDBCProvider:%s/DataSource:%s/"
        % (
            CELL_NAME,
            NODE_NAME,
            SERVER_NAME,
            JDBC_PROVIDER_NAME,
            DATA_SOURCE_NAME
        )
    )


def main():
    print "Locating DataSource " + DATA_SOURCE_NAME
    data_source_id = AdminConfig.getid(data_source_configuration_path())
    if not data_source_id:
        raise Exception(
            DATA_SOURCE_NAME + " was not found; run configure-websphere.sh first"
        )

    print "Requesting a connection test from WebSphere"
    result = AdminControl.testConnection(data_source_id)
    print result
    print "The shell wrapper now checks the server log for DSRA8030I confirmation."


main()
