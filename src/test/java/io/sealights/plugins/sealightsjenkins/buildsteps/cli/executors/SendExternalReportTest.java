package io.sealights.plugins.sealightsjenkins.buildsteps.cli.executors;

import hudson.EnvVars;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.entities.BaseCommandArguments;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.entities.ExternalReportCommandArguments;
import io.sealights.plugins.sealightsjenkins.utils.Logger;
import io.sealights.plugins.sealightsjenkins.utils.NullLogger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SendExternalReportTest {

    private Logger nullLogger = new NullLogger();

    @Test
    public void execute_giveValidExternalReportArguments_shouldExecuteCorrectCommand() throws IOException {
        //Arrange
        BaseCommandArguments baseCommandArguments = createBaseCommandArguments();
        ExternalReportCommandArguments externalReportArguments = createExternalReportArguments();
        ExternalReportCommandExecutor externalReportExecutor = new ExternalReportCommandExecutor(nullLogger, baseCommandArguments, externalReportArguments);


        Runtime runtimeMock = mock(Runtime.class);

        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

        //Act
        externalReportExecutor.setRuntime(runtimeMock);
        externalReportExecutor.execute();
        verify(runtimeMock).exec(captor.capture());
        final String[] actualCommandLine = captor.getValue();
        String[] expectedCommandLine = {"path/to/java", "-jar", "agent.jar", "externalReport", "-token", "fake-token", "-appname", "demoApp", "-buildname", "1", "-branchname", "branchy", "-report", "fake-report"};

        // Assert
        Assert.assertArrayEquals(
                "The command line that was executed for the 'external report' executor is not as expected",
                expectedCommandLine, actualCommandLine);
    }

    @Test
    public void execute_runtimeProcessThrowsException_shouldEndQuietly() throws IOException {
        //Arrange
        BaseCommandArguments baseCommandArguments = createBaseCommandArguments();
        ExternalReportCommandArguments externalReportArguments = createExternalReportArguments();
        ExternalReportCommandExecutor externalReportExecutor = new ExternalReportCommandExecutor(nullLogger, baseCommandArguments, externalReportArguments);

        Runtime runtimeMock = mock(Runtime.class);
        when(runtimeMock.exec(any(String.class))).thenThrow(new IOException());

        //Act
        externalReportExecutor.setRuntime(runtimeMock);
        try {
            boolean result = externalReportExecutor.execute();
            Assert.assertFalse("externalReportExecutor.execute() should be false!", result);
        } catch (Exception e) {
            Assert.fail("externalReportExecutor.execute() should not throw exception!");
        }
    }

    private ExternalReportCommandArguments createExternalReportArguments() {
        ExternalReportCommandArguments externalReportArguments = new ExternalReportCommandArguments(
                "fake-report"
        );

        return externalReportArguments;
    }

    private BaseCommandArguments createBaseCommandArguments() {
        BaseCommandArguments baseCommandArguments = new BaseCommandArguments();
        baseCommandArguments.setJavaPath("path/to/java");
        baseCommandArguments.setAgentPath("agent.jar");
        baseCommandArguments.setToken("fake-token");
        baseCommandArguments.setAppName("demoApp");
        baseCommandArguments.setBuildName("1");
        baseCommandArguments.setBranchName("branchy");
        baseCommandArguments.setEnvVars(new EnvVars());
        return baseCommandArguments;
    }

}
