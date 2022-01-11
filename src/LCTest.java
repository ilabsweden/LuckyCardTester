// SPDX-FileCopyrightText: 2022 Erik Billing <erik.billing@his.se>
//
// SPDX-License-Identifier: GPL-3.0-or-later

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LCTest {

	Runtime runtime = Runtime.getRuntime();
	private String applicationPath;

	public LCTest(String applicationPath) {
		this.applicationPath = applicationPath;
	}

	public static void main(String[] args) {
		if (args.length<1) {
			System.out.println("LCTest is a test utility for the LuckyCard assignmet (IT401G).\n\nUsage: java LCTest.java <path to your project folder. Use '.' to indicate the current folder>");
			return;
		}
		String path = args[0];
		LCTest t = new LCTest(path);

//		Map<String, String> env = System.getenv();
//		for (String envName : env.keySet()) {
//		     System.out.format("%s=%s%n", envName, env.get(envName));
//		}

		try {
			t.testAll();
		} catch (TestException | IOException | InterruptedException e) {
			System.out.println("Test failed with error:");
			e.printStackTrace();
		}
	}

	Process exec(String... args) throws IOException {
		return runtime.exec(args, null, new File(applicationPath));
	}

	BufferedReader stdin(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	BufferedReader stderr(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

	void stdout(Process p, String message) throws IOException {
		OutputStream out = p.getOutputStream();
		PrintStream stdout = new PrintStream(out);
		stdout.print(message);
		//stdout.print("\n");
		stdout.flush();
	}

	List<String> readAll(BufferedReader reader) {
		List<String> lines = new ArrayList<String>();
		String line;
		try {
			while (reader.ready())
				lines.add(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	void printAll(BufferedReader reader) {
		for (String line : readAll(reader)) {
			System.out.println(line);
		}
	}

	void printAll(List<String> lines) {
		for (String line : lines) {
			System.out.println(line);
		}
	}

	public Set<String> listSrcFiles() {
		return Stream.of(new File(applicationPath + "/src").listFiles()).filter(file -> !file.isDirectory())
				.map(File::getName).collect(Collectors.toSet());
	}

	int testJavac() throws InterruptedException, IOException, TestException {
		System.out.print("Verifying Java compiler... ");

		Process p;
		try {
			p = exec("javac", "--version");
		} catch (IOException e) {
			throw new TestException("javac not found!");
		}
		p.waitFor();
		BufferedReader reader = stdin(p);
		String line = reader.readLine();
		if (line.startsWith("javac ")) {
			System.out.println("Ok.");
			return 0;
		} else {
			throw new TestException("javac not found!");
		}

	}

	int testRun() throws IOException, InterruptedException, TestException {
		System.out.println("Running application... ");
		int warnings = 0;
		Process p = exec("java", "-cp", "bin", "Game");

		for (int i = 0; i < 2; i++) {
			if (p.waitFor(500, TimeUnit.MILLISECONDS)) {
				printAll(stdin(p));
				throw new TestException("Application closed unexpectedly!");
			}
			List<String> lines = readAll(stdin(p));
			printAll(lines);
			warnings += testOutput(lines, i==0);
			stdout(p, i == 1 ? "q" : "\n");
		}

		if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
			throw new TestException("App did not close as expected!");
		}
		return warnings;
	}

	int testOutput(List<String> lines, boolean firstGame) {
		int warnings = 0;
		int i = 0;

		while (i < lines.size() && lines.get(i).isEmpty())
			i++;
		if (firstGame) {
			if (i >= lines.size() || !lines.get(i).contains("Welcome")) {
				System.out.println("Warning: Expected welcome message at line 1!");
				warnings++;
			}
			i++;
		}
		
		while (i < lines.size() && lines.get(i).isEmpty())
			i++;
		if (i < lines.size() && lines.get(i).contains("Playing")) {
			if (lines.size() >= i + 4) {
				int card1 = card(lines.get(i+1),1);
				int card2 = card(lines.get(i+2),2);
				int card3 = card(lines.get(i+3),3);
			} else {
				System.out.println("Expected four lines of output from current game, found:");
				for (int j = i + 1; j < lines.size(); j++)
					System.out.println(lines.get(i));
				warnings++;
			}
		} else {
			System.out.println("Warning: Expected lines presening the current game round!");
		}

		if (!lines.get(lines.size() - 1).contains("Press ENTER to play again or")) {
			System.out.println("Warning: Expected message at last line: \"Press ENTER to play again or 'q' to quit:\"");
			warnings++;
		}
		return warnings;
	}

	int testCompile() throws InterruptedException, IOException, TestException {
		System.out.print("Compiling application... ");

		Process p = exec("javac", "-d", "bin", "-sourcepath", "src", "src/Game.java");
		p.waitFor();
		List<String> errors = readAll(stderr(p));
		if (errors.isEmpty()) {
			System.out.println("Ok.");
			return 0;
		} else {
			throw new TestException(String.join(" ", errors));
		}
	}

	int testProject() {
		System.out.print("Testing Eclipse project... ");
		if (new File(applicationPath + "/.project").exists()) {
			System.out.println("Ok.");
			return 0;
		} else {
			System.out.println("Warning! Eclipse project definition is missing!");
			return 1;
		}
	}

	int testSrc() {
		System.out.print("Testing source... ");
		if (new File(applicationPath + "/src/Game.java").exists()
				&& new File(applicationPath + "/src/Deck.java").exists()
				&& new File(applicationPath + "/src/Card.java").exists()) {
			System.out.println("Ok.");
			return 0;
		} else {
			System.out.println(
					"Warning! \nYour application should comprise three java files located in the src folder of your project: Card.java, Deck.java, Game.java. Some of these files could not be found!");
			return 1;
		}
	}

	int testAll() throws InterruptedException, IOException, TestException {
		int warnings = 0;
		warnings += testJavac();
		warnings += testProject();
		warnings += testSrc();
		warnings += testCompile();
		warnings += testRun();
		if (warnings == 0) {
			System.out.println("All tests ok.");
		} else {
			System.out.println("Tests completed with " + warnings + " warnings.");
		}
		return warnings;
	}
	
	int card(String s, int cardIndex) {
		if (!s.startsWith("Card")) {
			System.out.println("Expected presentation of card " + cardIndex + ", found:\n" + s);
			return 1;
		}
		String[] parts = s.split(":|->|=");
		if (parts.length==4) {
			int value = cardValue(parts[1]);
			int specifiedValue = Integer.parseInt(parts[3].trim());
			if (value != specifiedValue) {
				System.out.println("Warning: Card value is " + value + " but was specified to " + specifiedValue + "!");
			}
			return value;
		} else {
			System.out.println("Warning: Unrecognized card format: " + s);
			return 0;
		}
	}
	
	int cardValue(String s) {
		s = s.trim();
		int v = s.contains("Diamonds") ? 4 : s.contains("Clubs") ? 6 : s.contains("Hearts") ? 8 : s.contains("Spades") ? 10 : 0;
		String[] parts = s.split(" ");
		if (parts.length > 1) {
			v += Integer.parseInt(parts[1]);
		}
		return v;
	}
}

class TestException extends Exception {
	private static final long serialVersionUID = 1L;

	public TestException(String message) {
		super(message);
	}
}
