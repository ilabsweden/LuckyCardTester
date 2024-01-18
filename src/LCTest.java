// SPDX-FileCopyrightText: 2022 Erik Billing <erik.billing@his.se>
//
// SPDX-License-Identifier: GPL-3.0-or-later

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Application tester for the LuckyCard assignment (IT401G, University of
 * Skovde).
 * 
 * @author Erik Billing
 *
 */
public class LCTest {
	String MAIN_SRC = "src/Game.java";

	Runtime runtime = Runtime.getRuntime();
	private String projectPath;
	private static String encoding = "Windows-1252";

	private static final String sep = System.getProperty("file.separator");

	/**
	 * Instantiates a test for a single project.
	 * 
	 * @param projectPath specifies the path to the project to be tested.
	 */
	public LCTest(String projectPath) {
		this.projectPath = projectPath;
	}

	public static void main(String[] args) {
		String path = parseArgs(args);
		if (path == null) {
			System.out.println(
					"LCTest is a test utility for the LuckyCard assignment (IT401G).\n\nUsage:\n\tjava LCTest.java <path to your project folder. Use '.' to indicate the current folder>\n\tjava LCTest.java -encoding UTF-8 <path> //for code written on OSX/Linux");
			return;
		}
		
		LCTest t = new LCTest(path);

		try {
			t.testAll();
		} catch (TestException | IOException | InterruptedException e) {
			System.out.println("Test failed with error: ");
			e.printStackTrace();
		}
	}

	private static String parseArgs(String[] args) {
		String path = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-help":
				return null;
			case "-encoding":
				encoding = i + 1 < args.length ? args[i + 1] : encoding;
				i++;
			default:
				path = args[i];
			}
		}
		return path;
	}

	/**
	 * Convenience method for executing a sub process.
	 * 
	 * @param args array of arguments to runtime.
	 * @return the executed sub-process.
	 * @throws IOException
	 */
	Process exec(String... args) throws IOException {
		return runtime.exec(args, null, new File(projectPath));
	}

	/**
	 * Returns a BufferedReader over stdout for specified process p.
	 * 
	 * @param p a Process
	 * @return
	 */
	BufferedReader stdout(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	/**
	 * Returns a BufferedReader over stderr for specified process p.
	 * 
	 * @param p a Process
	 * @return
	 */
	BufferedReader stderr(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

	/**
	 * Writes to stdin of specified process p.
	 * 
	 * @param p       a Process
	 * @param message the message to be written to stdin.
	 * @throws IOException
	 */
	void stdin(Process p, String message) throws IOException {
		OutputStream out = p.getOutputStream();
		PrintStream stdout = new PrintStream(out);
		stdout.print(message);
		// stdout.print("\n");
		stdout.flush();
	}

	/**
	 * Reads all available data from specified reader.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	List<String> readAll(BufferedReader reader) throws IOException {
		List<String> lines = new ArrayList<>();
		CharBuffer buf = CharBuffer.allocate(2000);
		int read = reader.read(buf);
		if (read > 0) {
			lines.addAll(Arrays.asList(buf.flip().toString().split(System.lineSeparator())));
			if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty())
				lines.remove(lines.size() - 1);
		}
		return lines;
		// return reader.rea lines().collect(Collectors.toList());
	}

	/**
	 * Prints all data from specified reader to stdout.
	 * 
	 * @param reader
	 * @throws IOException
	 */
	void printAll(BufferedReader reader) throws IOException {
		for (String line : readAll(reader)) {
			System.out.println(line);
		}
	}

	/**
	 * Prints specified list of strings to stdout.
	 * 
	 * @param lines
	 */
	void printAll(List<String> lines) {
		for (String line : lines) {
			System.out.println(line);
		}
	}

	/**
	 * @return a list of source files for the project.
	 */
	public Set<String> listSrcFiles() {
		return Stream.of(new File(projectPath + "/src").listFiles()).filter(file -> !file.isDirectory())
				.map(File::getName).collect(Collectors.toSet());
	}

	/**
	 * Test existence of Java compiler.
	 * 
	 * @return number of warnings
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws TestException
	 */
	int testJavac() throws InterruptedException, IOException, TestException {
		System.out.print("Verifying Java compiler... ");

		Process p;
		try {
			p = exec("javac", "--version");
		} catch (IOException e) {
			throw new TestException("javac not found!");
		}
		p.waitFor();
		BufferedReader reader = stdout(p);
		String line = reader.readLine();
		if (line != null && line.startsWith("javac ")) {
			System.out.println("OK.");
			return 0;
		} else {
			throw new TestException("javac not found!");
		}

	}

	/**
	 * Tests that the project executes with expected inputs and outputs.
	 * 
	 * @return number of warnings.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws TestException
	 */
	int testRun(int gameCount) throws IOException, InterruptedException, TestException {
		System.out.println("Running application... ");
		int warnings = 0;
		Process p = exec("java", "-cp", "bin", "Main");

		for (int i = 1; i <= gameCount; i++) {
			if (p.waitFor(500, TimeUnit.MILLISECONDS)) {
				printAll(stdout(p));
				throw new TestException("Application closed unexpectedly!");
			}
			List<String> lines = readAll(stdout(p));
			printAll(lines);
			warnings += testOutput(lines, i == 1);
			stdin(p, i == gameCount ? "q\n" : "\n");
		}

		if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
			throw new TestException("App did not close as expected!");
		}
		return warnings;
	}

	/**
	 * Test specified outputs from a LuckyCard game.
	 * 
	 * @param lines     the output as a list of strings.
	 * @param firstGame
	 * @return number of warnings.
	 */
	int testOutput(List<String> lines, boolean firstGame) {
		int warnings = 0;
		int i = skipEmpty(lines, 0);

		if (firstGame) {
			if (i >= lines.size() || !lines.get(i).contains("Welcome")) {
				System.out.println("Warning: Expected welcome message at line 1!");
				warnings++;
			}
			i++;
		}

		i += skipEmpty(lines, i);
		if (i < lines.size() && lines.get(i).contains("Playing")) {
			if (lines.size() > i + 4) {
				int[] cards = new int[3];
				for (int c = 1; c <= 3; c++) {
					try {
						cards[c - 1] = card(lines.get(i + c), c);
					} catch (TestException e) {
						warnings++;
						System.out.println(e.getMessage());
					}
				}
				warnings += testWinMessage(lines, i + 4, cards);
			} else {
				System.out.println("Expected four lines of output from current game, Found: ");
				for (int j = i + 1; j < lines.size(); j++)
					System.out.println(lines.get(i));
				warnings++;
			}
		} else {
			System.out.println("Warning: Expected lines presenting the current game round!");
		}

		if (!lines.get(lines.size() - 1).contains("Press ENTER to play again or")) {
			System.out.println("Warning: Expected message at last line: \"Press ENTER to play again or 'q' to quit:\"");
			warnings++;
		}
		return warnings;
	}

	/**
	 * Test the win/lose message
	 * 
	 * @param lines output lines from the application
	 * @param i     index of current line to look for win message
	 * @param cards the three cards drawn
	 * @return number of warnings.
	 */
	int testWinMessage(List<String> lines, int i, int... cards) {
		i += skipEmpty(lines, i);
		String winMessage = lines.get(i);
		if ((cards[0] < cards[2] && cards[1] > cards[2]) || (cards[0] > cards[2] && cards[1] < cards[2])) {
			if (winMessage.trim().equals("You win!")) {
				return 0;
			} else {
				System.out.println("Warning: Incorrect game result (expected \"You win!\"), found: " + winMessage);
				return 1;
			}
		} else {
			if (winMessage.trim().equals("You lose!")) {
				return 0;
			} else {
				System.out.println("Warning: Incorrect game result (expected \"You lose!\"), found: " + winMessage);
				return 1;
			}
		}
	}

	/**
	 * Verifies that the project compiles.
	 * 
	 * @return number of warnings.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws TestException
	 */
	int testCompile() throws InterruptedException, IOException, TestException {
		System.out.print("Compiling application... ");

		Process p = exec("javac", "-encoding", encoding, "-d", "bin", "-sourcepath", "src", MAIN_SRC);
		p.waitFor();
		List<String> errors = readAll(stderr(p));
		if (errors.isEmpty()) {
			System.out.println("OK");
			return 0;
		} else {
			throw new TestException(String.join(" ", errors));
		}
	}

	/**
	 * Verifies the existence of an Eclipse project.
	 * 
	 * @return number of warnings.
	 */
	int testProject() {
		System.out.print("Testing Eclipse project... ");
		if (new File(projectPath + "/.project").exists()) {
			System.out.println("OK");
			return 0;
		} else {
			System.out.println("Warning! Eclipse project definition is missing!");
			return 1;
		}
	}

	/**
	 * Verifies that the expected source files are present in the default package.
	 * 
	 * @return number of warnings.
	 */
	int testSrc() {
		System.out.print("Testing source... ");
		if (new File(projectPath + "/src/Game.java").exists() && new File(projectPath + "/src/Deck.java").exists()
				&& new File(projectPath + "/src/Card.java").exists()) {
			System.out.println("OK");
			return 0;
		} else {
if(!new File(projectPath + "/src/Game.java").exists())
System.out.println("Game.java is missing");
if(!new File(projectPath + "/src/Card.java").exists())
System.out.println("Card.java is missing");
if(!new File(projectPath + "/src/Deck.java").exists())
System.out.println("Deck.java is missing");
			System.out.println(
					"Warning! \nYour application should consist of three java files located in the src folder of your project: Card.java, Deck.java, Game.java. Some of these files could not be found!");
			return 1;
		}
	}

	/**
	 * Runs all tests.
	 * 
	 * @return number of warnings.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws TestException
	 */
	int testAll() throws InterruptedException, IOException, TestException {
		int warnings = 0;
		warnings += testJavac();
		warnings += testProject();
		warnings += testSrc();
		warnings += testCompile();
		warnings += testRun(2);
		if (warnings == 0) {
			System.out.println("All tests OK.");
		} else {
			System.out.println("Tests completed with " + warnings + " warnings.");
		}
		return warnings;
	}

	/**
	 * Verifies that the specified string s match the output for a card.
	 * 
	 * @param s         String to test
	 * @param cardIndex is the card index in the LuckyCard game (1, 2, or 3).
	 * @return specified card value.
	 * @throws TestException
	 */
	int card(String s, int cardIndex) throws TestException {
		if (!s.startsWith("Card")) {
			throw new TestException("Warning: Expected presentation of card " + cardIndex + ", found:\n" + s);
		}
		String[] parts = s.split(":|->|\\?|→|=");
		if (parts.length == 4) {
			int value = cardValueWithBonus(parts[1]);
			int specifiedValue = Integer.parseInt(parts[3].trim());
			if (value != specifiedValue) {
				throw new TestException(
						"Warning: Card value is " + value + " but was specified to " + specifiedValue + "!");
			}
			return value;
		} else {
			throw new TestException("Warning: Unrecognized card format: " + s);
		}
	}

	/**
	 * Calculates card value, including bonus.
	 * 
	 * @param s String to parse
	 * @return the total value for a card, including bonus.
	 */
	int cardValueWithBonus(String s) {
		s = s.trim();
		int v = s.contains("Diamonds") ? 4
				: s.contains("Clubs") ? 6 : s.contains("Hearts") ? 8 : s.contains("Spades") ? 10 : 0;
		String[] parts = s.split(" ");
		if (parts.length > 1) {
			v += cardValue(parts[1]);
		}
		return v;
	}

	int cardValue(String v) {
		switch (v) {
		case "A":
			return 1;
		case "K":
			return 13;
		case "Q":
			return 12;
		case "J":
			return 11;
		default:
			return Integer.parseInt(v);
		}
	}

	int skipEmpty(List<String> lines, int startIndex) {
		int skip = 0;
		while (startIndex + skip < lines.size()
				&& (lines.get(startIndex + skip) == null || lines.get(startIndex + skip).trim().isEmpty())) {
			skip++;
		}
		return skip;
	}
}

class TestException extends Exception {
	private static final long serialVersionUID = 1L;

	public TestException(String message) {
		super(message);
	}
}
