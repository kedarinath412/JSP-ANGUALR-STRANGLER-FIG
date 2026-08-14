data_source = AdminConfig.getid("/Cell:DefaultCell01/Node:DefaultNode01/Server:server1/JDBCProvider:Legacy POC PostgreSQL JDBC Provider/DataSource:LegacyPocDS/")
if not data_source:
    raise Exception("LegacyPocDS configuration was not found")

print AdminControl.testConnection(data_source)
