APP_NAME = "legacy-poc-ear"
EAR_PATH = "/work/app/legacy-poc-ear.ear"

existing = AdminConfig.getid("/Deployment:%s/" % APP_NAME)
manager = AdminControl.queryNames("type=ApplicationManager,process=server1,*")
if existing:
    running = AdminControl.queryNames("type=Application,name=%s,*" % APP_NAME)
    if manager and running:
        AdminControl.invoke(manager, "stopApplication", APP_NAME)
        print "Stopped application for update: " + APP_NAME
    AdminApp.update(APP_NAME, "app", ["-operation", "update", "-contents", EAR_PATH])
    AdminConfig.save()
    print "Updated application: " + APP_NAME
else:
    AdminApp.install(EAR_PATH, [
        "-appname", APP_NAME,
        "-usedefaultbindings",
        "-MapModulesToServers",
        [[".*", ".*", "WebSphere:cell=DefaultCell01,node=DefaultNode01,server=server1"]]
    ])
    AdminConfig.save()
    print "Installed application: " + APP_NAME

if manager:
    AdminControl.invoke(manager, "startApplication", APP_NAME)
    print "Started application: " + APP_NAME
