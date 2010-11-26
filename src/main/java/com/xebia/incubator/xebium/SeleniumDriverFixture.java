package com.xebia.incubator.xebium;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriverCommandProcessor;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.SeleniumException;

public class SeleniumDriverFixture {

	private static Logger LOG = Logger.getLogger(SeleniumDriverFixture.class);
	
	private CommandProcessor commandProcessor;

	// TODO: decide: move to method or move to separate fixture.
	static {
		BasicConfigurator.configure();
	}

	public CommandProcessor detectWebDriverCommandProcessor(final String browser, String browserUrl) {
		browserUrl = FitNesseUtil.removeAnchorTag(browserUrl);
		Capabilities capabilities;
		
		if ("firefox".equalsIgnoreCase(browser)) {
			capabilities = DesiredCapabilities.firefox();
		} else if ("iexplore".equalsIgnoreCase(browser)) {
			capabilities = DesiredCapabilities.internetExplorer();
		} else if ("chrome".equalsIgnoreCase(browser)) {
			capabilities = DesiredCapabilities.chrome();
		} else if ("htmlUnit".equalsIgnoreCase(browser)) {
			capabilities = DesiredCapabilities.htmlUnit();
		} else {
			throw new RuntimeException("Unknown browser type. Should be one of 'firefox', 'iexplore', 'chrome' or 'htmlUnit'");
		}
		return new WebDriverCommandProcessor(browserUrl, capabilities);
	}
	
	public void startBrowserOnUrl(final String browser, final String browserUrl) {
        commandProcessor = detectWebDriverCommandProcessor(browser, browserUrl);
        commandProcessor.start();
        LOG.debug("Started command processor");
	}

	public void startBrowserOnHostOnPortOnUrl(final String browserStartCommand, final String serverHost, final int serverPort, final String browserUrl) {
		commandProcessor = new HttpCommandProcessor(serverHost, serverPort, browserStartCommand, FitNesseUtil.removeAnchorTag(browserUrl));
		commandProcessor.start();
        LOG.debug("Started HTML command processor");
	}
	
	public boolean doOn(final String command, final String target) {
		LOG.info("Performing | " + command + " | " + target + " |");
		return executeDoCommand(command, new String[] { target });
	}
	
	public boolean doOnWith(final String command, final String target, final String value) {
		LOG.info("Performing | " + command + " | " + target + " | " + value + " |");
		return executeDoCommand(command, new String[] { target, value });
	}

	public String is(final String command) {
		return executeCommand(command, new String[] { });
	}
	
	public String isOn(final String command, final String target) {
		return executeCommand(command, new String[] { target });
	}

	private String executeCommand(final String methodName, final String[] values) {
		return executeCommand(new ExtendedSeleniumCommand(methodName), values);
	}
	
	private boolean executeDoCommand(final String methodName, final String[] values) {
		ExtendedSeleniumCommand command = new ExtendedSeleniumCommand(methodName);
		String output = executeCommand(command, values);

		if (command.isVerifyCommand() || command.isWaitForCommand()) {
			return checkResult(output, command, values[values.length - 1]);
		} else if (command.isAssertCommand()) {
			if (!checkResult(output, command, values[values.length - 1])) {
				throw new AssertionError(output);
			}
		}
		return true;
	}

	private String executeCommand(final ExtendedSeleniumCommand command, final String[] values) {
		if (commandProcessor == null) {
			throw new IllegalStateException("Command processor not running. First start it by invoking startBrowserOnUrl");
		}
		String output = null;
		try {
			output = commandProcessor.doCommand(command.getSeleniumCommand(), values);

			if (output != null && LOG.isDebugEnabled()) {
				LOG.debug("Command processor returned '" + output + "'");
			}

			if (command.isAndWaitCommand()) {
				commandProcessor.doCommand("waitForPageToLoad", new String[] {});
			}
		} catch (SeleniumException e) {
			LOG.error("Execution of command failed: " + e.getMessage());
		}
		return output;
	}

	private boolean checkResult(String output, ExtendedSeleniumCommand command,
			final String values) {
		if (command.isNegateCommand()) {
			return command.isBooleanCommand() ? "false".equals(output) : !values.equals(output);
		} else {
			return command.isBooleanCommand() ? "true".equals(output) : values.equals(output);
		}
	}

	public void stopBrowser() {
        this.commandProcessor.stop();
        this.commandProcessor = null;
        LOG.info("Command processor stopped");
	}


}
