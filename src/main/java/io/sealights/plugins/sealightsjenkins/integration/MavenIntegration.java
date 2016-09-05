package io.sealights.plugins.sealightsjenkins.integration;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import io.sealights.plugins.sealightsjenkins.entities.FileBackupInfo;
import io.sealights.plugins.sealightsjenkins.integration.plugins.SealightsMavenPluginIntegrator;
import io.sealights.plugins.sealightsjenkins.integration.plugins.external.LazerycodeJMeterPluginIntegrator;
import io.sealights.plugins.sealightsjenkins.utils.Logger;

import java.io.IOException;


/**
 * Created by Nadav on 4/19/2016.
 */
public class MavenIntegration {
    private final static String SEALIGHTS_ARTIFACT_ID = "sealights-maven-plugin";


    private MavenIntegrationInfo mavenIntegrationInfo;
    private Logger log;
    private boolean isJenkinsEnvironment;

    public MavenIntegration(Logger log, MavenIntegrationInfo mavenIntegrationInfo) {
        this(log, mavenIntegrationInfo, true);
    }

    public MavenIntegration(Logger log, MavenIntegrationInfo mavenIntegrationInfo, boolean isJenkinsEnvironment) {
        this.log = log;
        this.mavenIntegrationInfo = mavenIntegrationInfo;
        this.isJenkinsEnvironment = isJenkinsEnvironment;
    }

    public void integrate() {
        this.integrate(true);
    }

    public void integrate(boolean shouldBackup) {

        log.info("MavenIntegration.integrate - starting");

        for (FileBackupInfo fileBackupInfo : mavenIntegrationInfo.getPomFiles()) {
            String sourceFilename = fileBackupInfo.getSourceFile();
            try {
                integrateToPomFile(fileBackupInfo, sourceFilename, shouldBackup);
            } catch (Exception e) {
                log.error("MavenIntegration.integrate - Unable to integrate sealights to pom : " + sourceFilename + ". Error:", e);
            }
        }

    }

    private void integrateToPomFile(FileBackupInfo fileBackupInfo, String sourceFilename, boolean shouldBackup) throws IOException, InterruptedException {
        PomFile pomFile = createPomFile(sourceFilename);

        if (!shouldIntegrateToPom(pomFile)) {
            log.info("Skipping the Sealights integration with the pom '" + fileBackupInfo.getSourceFile() + "'.");
            return;
        }

        if (shouldBackup) {
            backupPom(sourceFilename);
        }

        log.info("MavenIntegration.integrateToPomFile - About to modify pom: " + fileBackupInfo.getSourceFile());
        integrateToAllProfiles(fileBackupInfo, pomFile);
    }

    private boolean shouldIntegrateToPom(PomFile pomFile) {
        boolean shouldIntegrate = true;
        if (pomFile.isPluginExistInEntirePom(SEALIGHTS_ARTIFACT_ID)) {
            log.info("MavenIntegration.shouldIntegrateToPom - " +
                    "SeaLights plugin is already defined in the the POM file. " +
                    "Should skip Sealights integration.");
            shouldIntegrate = false;
        }

        if (!pomFile.isValidPom()) {
            log.info("MavenIntegration.shouldIntegrateToPom - invalid pom. " +
                    "Should skip Sealights integration.");
            shouldIntegrate = false;
        }

        LazerycodeJMeterPluginIntegrator lazerycodeJMeterPluginIntegrator
                = new LazerycodeJMeterPluginIntegrator(log, mavenIntegrationInfo.getSeaLightsPluginInfo(), pomFile);
        if (lazerycodeJMeterPluginIntegrator.isAlreadyIntegrated()) {
            log.info("MavenIntegration.shouldIntegrateToPom - " +
                    "Sealights is already integrated in '" + lazerycodeJMeterPluginIntegrator.pluginDescriptor() + "' plugin. " +
                    "Should skip Sealights integration.");
            shouldIntegrate = false;
        }

        return shouldIntegrate;
    }

    private PomFile createPomFile(String sourceFilename) {
        if (isJenkinsEnvironment)
            return new JenkinsPomFile(sourceFilename, log);
        return new PomFile(sourceFilename, log);
    }

    private void backupPom(String sourceFileName) throws IOException, InterruptedException {
        String backupFile = sourceFileName + ".slbak";
        log.info("MavenIntegration.backupPom - creating a back up file: " + backupFile);

        VirtualChannel channel = Computer.currentComputer().getChannel();
        FilePath sourceFile = new FilePath(channel, sourceFileName);
        FilePath targetFile = new FilePath(channel, backupFile);

        sourceFile.copyTo(targetFile);
    }

    private void integrateToAllProfiles(FileBackupInfo fileBackupInfo, PomFile pomFile) {
        SeaLightsPluginInfo pluginInfo = mavenIntegrationInfo.getSeaLightsPluginInfo();
        String overrideSlMvnVersion = mavenIntegrationInfo.getOverridePluginVersion();

        SealightsMavenPluginIntegrator sealightsMavenPluginIntegrator
                = new SealightsMavenPluginIntegrator(log, pluginInfo, overrideSlMvnVersion, pomFile);
        sealightsMavenPluginIntegrator.integrateSafe();

        LazerycodeJMeterPluginIntegrator jmeterPluginIntegratorLazerycode
                = new LazerycodeJMeterPluginIntegrator(log, pluginInfo, pomFile);
        jmeterPluginIntegratorLazerycode.integrateSafe();

        pomFile.verifySurefireArgLineModificationSafe();
        savePom(fileBackupInfo, pomFile);
    }

    private void savePom(FileBackupInfo fileBackupInfo, PomFile pomFile) {
        String targetFile = fileBackupInfo.getTargetFile();
        if (targetFile == null || targetFile.equals("")) {
            targetFile = fileBackupInfo.getSourceFile();
        }
        savePom(targetFile, pomFile);
    }

    private void savePom(String filename, PomFile pomFile) {
        try {
            pomFile.save(filename);
        } catch (Exception e) {
            log.error("Failed saving POM file. Error:", e);
        }
    }

}