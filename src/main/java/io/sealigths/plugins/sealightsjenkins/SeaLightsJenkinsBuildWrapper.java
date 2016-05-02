package io.sealigths.plugins.sealightsjenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ComboBoxModel;
import hudson.util.DescribableList;
import hudson.util.ListBoxModel;
import io.sealigths.plugins.sealightsjenkins.integration.JarsHelper;
import io.sealigths.plugins.sealightsjenkins.integration.MavenIntegration;
import io.sealigths.plugins.sealightsjenkins.integration.MavenIntegrationInfo;
import io.sealigths.plugins.sealightsjenkins.integration.SeaLightsPluginInfo;
import io.sealigths.plugins.sealightsjenkins.language.Language;
import io.sealigths.plugins.sealightsjenkins.utils.StringUtils;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class SeaLightsJenkinsBuildWrapper extends BuildWrapper {

//    private final TechIntegration integrations;

    private final String appName;
    private final String moduleName;
    private final String branch;

    private final boolean overrideJars;
    private final boolean multipleBuildFiles;

    private final String pomPath;
    private final String environment;
    private final String packagesIncluded;
    private final String packagesExcluded;
    private final String filesIncluded;
    private final String filesExcluded;
    private final String relativePathToEffectivePom;
    private final boolean recursive;
    private final String workspacepath;
    private final String buildScannerJar;
    private final String testListenerJar;
    private final String apiJar;
    private final String testListenerConfigFile;
//  private final boolean inheritedBuild;
    private boolean autoRestoreBuildFile;


    private final String buildFilesPatterns;
    private final String buildFilesFolders;

    private boolean logEnabled;
    private LogDestination logDestination = LogDestination.CONSOLE;
    private final String logFolder;

    private TestingFramework testingFramework = TestingFramework.TESTNG;
    private LogLevel logLevel = LogLevel.OFF;
//    private Language language = Language.JAVA;
    private ProjectType projectType = ProjectType.MAVEN;
    private BuildStrategy buildStrategy = BuildStrategy.ONE_BUILD;

    @DataBoundConstructor
    public SeaLightsJenkinsBuildWrapper(String appName, String moduleName, String branch, String pomPath,
                                        @NonNull TestingFramework testingFramework,
                                        String packagesIncluded, String packagesExcluded,
                                        String filesIncluded, String filesExcluded,
                                        String relativePathToEffectivePom, boolean recursive,
                                        String workspacepath, String testListenerConfigFile,
                                        String buildScannerJar, String testListenerJar, String apiJar,
                                        BuildStrategy buildStrategy, String environment, @NonNull ProjectType projectType,
                                        boolean logEnabled, @NonNull LogLevel logLevel, @NonNull LogDestination logDestination, String logFolder,
                                        boolean autoRestoreBuildFile,
                                        String buildFilesPatterns, String buildFilesFolders,
                                        boolean multipleBuildFiles, boolean overrideJars) throws IOException {

//        this.integrations = integrations;
        this.appName = appName;
        this.moduleName = moduleName;
        this.branch = branch;
        this.pomPath = pomPath;
        this.packagesIncluded = packagesIncluded;
        this.packagesExcluded = packagesExcluded;
        this.filesIncluded = filesIncluded;
        this.filesExcluded = filesExcluded;
        this.relativePathToEffectivePom = relativePathToEffectivePom;
        this.recursive = recursive;
        this.workspacepath = workspacepath;
        this.testListenerConfigFile = testListenerConfigFile;
        this.buildStrategy = buildStrategy;
        this.autoRestoreBuildFile = autoRestoreBuildFile;

        this.environment = environment;

        this.testingFramework = testingFramework;

        this.projectType = projectType;

        this.multipleBuildFiles = multipleBuildFiles;
        this.overrideJars = overrideJars;

//        this.language = language;

        this.buildFilesFolders = buildFilesFolders;
        this.buildFilesPatterns = buildFilesPatterns;

        this.logEnabled = logEnabled;
        this.logLevel = logLevel;
        this.logDestination = logDestination;
        this.logFolder = logFolder;

        if (isNullOrEmpty(buildScannerJar)) {
            //The user didn't specify a specify version of the scanner. Use an embedded one.
            buildScannerJar = JarsHelper.loadJarAndSaveAsTempFile("sl-build-scanner");
        }

        if (isNullOrEmpty(testListenerJar)) {
            //The user didn't specify a specify version of the test listener. Use an embedded one.
            testListenerJar = JarsHelper.loadJarAndSaveAsTempFile("sl-test-listener");
        }

        if (isNullOrEmpty(apiJar)) {
            //The user didn't specify a specify version of the test listener. Use an embedded one.
            apiJar = JarsHelper.loadJarAndSaveAsTempFile("sl-api");
        }

        this.buildScannerJar = buildScannerJar;
        this.testListenerJar = testListenerJar;
        this.apiJar = apiJar;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
                             BuildListener listener) throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();

        log(logger, "testing framework: " + testingFramework);
        log(logger, "-----------Sealights Jenkins Plugin Configuration--------------");
        log(logger, "branch: " + branch);
        log(logger, "appName:" + appName);
        log(logger, "moduleName:" + moduleName);
        log(logger, "recursive: " + recursive);
        log(logger, "workspacepath: " + workspacepath);
        log(logger, "environment: " + environment);
        //log(logger, "projectType:" +projectType);

        log(logger, "overrideJars: " + overrideJars);
        log(logger, "multipleBuildFiles: " + multipleBuildFiles);

        log(logger, "buildFilesFolders: " + buildFilesFolders  + " buildFilesPatterns: " + buildFilesPatterns);

        log(logger, "pomPath:" + pomPath);
        log(logger, "packagesIncluded:" + packagesIncluded);
        log(logger, "packagesExcluded:" + packagesExcluded);
        log(logger, "filesIncluded:" + filesIncluded);
        log(logger, "filesExcluded:" + filesExcluded);
        log(logger, "buildScannerJar:" + buildScannerJar);
        log(logger, "testListenerJar:" + testListenerJar);
        log(logger, "testListenerConfigFile :" + testListenerConfigFile);
        log(logger, "strategy: " + buildStrategy);
        log(logger, "apiJar:" + apiJar);
        log(logger, "project Type : " + projectType);
        log(logger, "LogEnabled:" + logEnabled);
        log(logger, "logDestination:" + logDestination);
        log(logger, "logLevel:" + logLevel);
        log(logger, "logFolder:" + logFolder);
        log(logger, "autoRestoreBuildFile:" + autoRestoreBuildFile);
        log(logger, "-----------Sealights Jenkins Plugin Configuration--------------");

        Environment env = new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
            }
        };

        FilePath ws = build.getWorkspace();
        if (ws == null) {
            return env;
        }

        if (this.autoRestoreBuildFile) {
            tryAddRestoreBuildFilePublisher(build, logger);
        }
        configureBuildFilePublisher(build);

        String workingDir = ws.getRemote();
        String pomPath;
        if (relativePathToEffectivePom != null && !"".equals(relativePathToEffectivePom))
            pomPath = workingDir + "/" + relativePathToEffectivePom;
        else
            pomPath = workingDir + "/pom.xml";

        log(logger, "Absolute path to effective file: " + pomPath);

        SeaLightsPluginInfo slInfo = new SeaLightsPluginInfo();
        slInfo.setEnabled(true);
        slInfo.setBuildName(String.valueOf(build.getNumber()));
        slInfo.setCustomerId(getDescriptor().getCustomerId());
        slInfo.setServerUrl(getDescriptor().getUrl());
        slInfo.setProxy(getDescriptor().getProxy());

        if (workspacepath != null && !"".equals(workspacepath))
            slInfo.setWorkspacepath(workspacepath);
        else
            slInfo.setWorkspacepath(workingDir);


        slInfo.setAppName(appName);
        slInfo.setModuleName(moduleName);
        slInfo.setBranchName(branch);
        slInfo.setFilesIncluded(filesIncluded);
        slInfo.setFilesExcluded(filesExcluded);
        slInfo.setRecursive(recursive);
        slInfo.setPackagesIncluded(packagesIncluded);
        slInfo.setPackagesExcluded(packagesExcluded);

        slInfo.setListenerJar(testListenerJar);
        slInfo.setListenerConfigFile(testListenerConfigFile);
        slInfo.setScannerJar(buildScannerJar);
        slInfo.setApiJar(apiJar);
        slInfo.setBuildStrategy(buildStrategy);

        slInfo.setEnvironment(environment);

        slInfo.setLogEnabled(!("Off".equalsIgnoreCase(logLevel.getDisplayName())));
        slInfo.setLogLevel(logLevel);
        slInfo.setLogDestination(logDestination);
        slInfo.setLogFolder(logFolder);

        String foldersToSearch = StringUtils.isNullOrEmpty(buildFilesFolders)? workingDir : buildFilesFolders;
        String patternsToSearch = StringUtils.isNullOrEmpty(buildFilesPatterns)? "*pom.xml" : buildFilesPatterns;

        slInfo.setBuildFilesFolders(foldersToSearch);
        slInfo.setBuildFilesPatterns(patternsToSearch);

        MavenIntegrationInfo info = new MavenIntegrationInfo(
                foldersToSearch,
                pomPath,
                slInfo,
                testingFramework
        );

        MavenIntegration mavenIntegration = new MavenIntegration(listener.getLogger(), info);
        mavenIntegration.integrate();

        return env;
    }

    private void  tryAddRestoreBuildFilePublisher(AbstractBuild build, PrintStream logger){
        DescribableList publishersList = build.getProject().getPublishersList();
        boolean found = false;
        for (Object item : publishersList) {
            if (item.toString().contains("RestoreBuildFile") ) {
                found = true;
                log(logger, "There was no need to add a new RestoreBuildFile since there is one. Current one:" + item.toString());
                //If found, this was added manually. Remove the check box.
                break;
            }
        }

        if (!found) {
            RestoreBuildFile restoreBuildFile = new RestoreBuildFile(true, buildFilesFolders, buildFilesPatterns);
            publishersList.add(restoreBuildFile);
        }
    }

    private void configureBuildFilePublisher(AbstractBuild build) {
        DescribableList publishersList = build.getProject().getPublishersList();
        for (Object item : publishersList) {
            if (item.toString().contains("RestoreBuildFile") ) {
                ((RestoreBuildFile)item).setFolders(buildFilesFolders);
                return;
            }
        }
    }

    private void log(PrintStream logger, String message) {
        message = "[SeaLights Jenkins Plugin] " + message;
        logger.println(message);
    }

    public DescriptorImpl getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance != null) {
            Descriptor desc = jenkinsInstance.getDescriptorOrDie(getClass());
            if (desc != null) {
                return (DescriptorImpl) desc;
            }
        }
        return new DescriptorImpl();
    }

//    public TechIntegration getIntegrations() {
//        return integrations;
//    }

    public String getAppName() {
        return appName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBranch() {
        return branch;
    }

//
//    public String getProjectType() {
//        return projectType;
//    }


//    public Language getLanguage() {
//        return language;
//    }
//
//    public void setLanguage(Language language) {
//        this.language = language;
//    }

    public String getPomPath() {
        return pomPath;
    }

    public String getPackagesIncluded() {
        return packagesIncluded;
    }

    public String getPackagesExcluded() {
        return packagesExcluded;
    }

    public String getFilesIncluded() {
        return filesIncluded;
    }

    public String getFilesExcluded() {
        return filesExcluded;
    }

    public String getRelativePathToEffectivePom() {
        return relativePathToEffectivePom;
    }

    public String getWorkspacepath() {
        return workspacepath;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String getBuildScannerJar() {
        return buildScannerJar;
    }

    public String getTestListenerJar() {
        return testListenerJar;
    }

    public String getTestListenerConfigFile() {
        return testListenerConfigFile;
    }

    public BuildStrategy getBuildStrategy() {
        return buildStrategy;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public LogDestination getLogDestination() {
        return logDestination;
    }

    public void setLogDestination(LogDestination logDestination) {
        this.logDestination = logDestination;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getLogFolder() {
        return logFolder;
    }

    public String getApiJar() {
        return apiJar;
    }

    public TestingFramework getTestingFramework() {
        return testingFramework;
    }

    public void setTestingFramework(TestingFramework testingFramework) {
        this.testingFramework = testingFramework;
    }

    private boolean isNullOrEmpty(String str) {
        return (str == null || str.equals(""));
    }

    public boolean isAutoRestoreBuildFile() {
        return autoRestoreBuildFile;
    }

    public void setAutoRestoreBuildFile(boolean autoRestoreBuildFile) {
        this.autoRestoreBuildFile = autoRestoreBuildFile;
    }

    public boolean isMultipleBuildFiles() {
        return multipleBuildFiles;
    }

    public boolean isOverrideJars() {
        return overrideJars;
    }

    public String getBuildFilesPatterns() {
        return buildFilesPatterns;
    }

    public String getBuildFilesFolders() {
        return buildFilesFolders;
    }

    public void setBuildStrategy(BuildStrategy buildStrategy) {
        this.buildStrategy = buildStrategy;
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        private String customerId;
        private String url;
        private String proxy;

        public DescriptorImpl() {
            super(SeaLightsJenkinsBuildWrapper.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Enable SeaLights integration";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            customerId = json.getString("customerId");
            url = json.getString("url");
            proxy = json.getString("proxy");
            save();
            return super.configure(req, json);
        }


        public String getUrl() {
            return url;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getProxy() {
            return proxy;
        }

        public void setProxy(String proxy) {
            this.proxy = proxy;
        }

        public ListBoxModel doFillProjectTypesItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("MAVEN", "");
            return items;
        }

        public ComboBoxModel doFillTheTypeItems(@QueryParameter ProjectType projectType) {
            switch (projectType.getDisplayName()) {
                case "maven":
                    return new ComboBoxModel("Come Together", "Something", "I Want You");
                default:
                    return new ComboBoxModel("The One After 909", "Rocker", "Get Back");
            }
        }

    }
}