"""Install or update the POC EAR and start it on WebSphere server1."""

APPLICATION_NAME = "legacy-poc-ear"
EAR_FILE_IN_CONTAINER = "/work/app/legacy-poc-ear.ear"
CELL_NAME = "DefaultCell01"
NODE_NAME = "DefaultNode01"
SERVER_NAME = "server1"


def application_manager():
    """Return the running WebSphere service that starts and stops applications."""
    return AdminControl.queryNames(
        "type=ApplicationManager,process=%s,*" % SERVER_NAME
    )


def application_is_installed():
    """Check the saved WebSphere configuration for this application."""
    deployment_path = "/Deployment:%s/" % APPLICATION_NAME
    return bool(AdminConfig.getid(deployment_path))


def application_is_running():
    """Check whether WebSphere currently has a runtime MBean for the app."""
    runtime_query = "type=Application,name=%s,*" % APPLICATION_NAME
    return bool(AdminControl.queryNames(runtime_query))


def stop_application_if_running(manager_id):
    if manager_id and application_is_running():
        print "Stopping the currently running application"
        AdminControl.invoke(manager_id, "stopApplication", APPLICATION_NAME)


def update_application():
    """Replace the installed application contents with the new EAR."""
    print "Updating existing application from " + EAR_FILE_IN_CONTAINER
    AdminApp.update(APPLICATION_NAME, "app", [
        "-operation", "update",
        "-contents", EAR_FILE_IN_CONTAINER
    ])
    AdminConfig.save()
    print "Application update saved"


def install_application():
    """Perform the first installation and map the WAR to server1."""
    print "Installing application from " + EAR_FILE_IN_CONTAINER
    deployment_target = (
        "WebSphere:cell=%s,node=%s,server=%s"
        % (CELL_NAME, NODE_NAME, SERVER_NAME)
    )
    AdminApp.install(EAR_FILE_IN_CONTAINER, [
        "-appname", APPLICATION_NAME,
        "-usedefaultbindings",
        "-MapModulesToServers",
        [[".*", ".*", deployment_target]]
    ])
    AdminConfig.save()
    print "Application installation saved"


def start_application(manager_id):
    if not manager_id:
        raise Exception("ApplicationManager was not found for " + SERVER_NAME)
    print "Starting application " + APPLICATION_NAME
    AdminControl.invoke(manager_id, "startApplication", APPLICATION_NAME)
    print "Application started successfully"


def main():
    manager_id = application_manager()

    if application_is_installed():
        stop_application_if_running(manager_id)
        update_application()
    else:
        install_application()

    start_application(manager_id)


main()
