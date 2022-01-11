<!--
SPDX-FileCopyrightText: 2022 Erik Billing <erik.billing@his.se>

SPDX-License-Identifier: GPL-3.0-or-later
-->

# LuckyCardTester
Application tester for the LuckyCard assignment (IT401G, University of Skövde).

This utility allow you to check your solution to the LuckyCard assignment before you submit. This tool is intended to help you verify that your solution meets some basic requirements, but it does not check _all_ requirements and the test result should thus not be read as a guarantee for a pass.

## Download
You may download LuckyCardTester as a zip-file by clicking the green button *Code* on the GitHub page. You may alternatively clone the repository using Git. After download you find LuckyCardTester in a single java-file named `LCTest.java`. This file can be executed directly by the Java JRE (no need to compile it first). 

## Usage: 
This utility has a text based interface meant to be used in a terminal. LuckyCardTester will compile and execute your project, any print the test result. 

`java LCTest.java`   _Prints help message._

`java LCTest.java .`   _Runs test in current folder._

`java LCTest.java <path to project folder>`   _Runs test for specified folder._

If you want, you can also add LCTest.java to your Eclipse project and create a custom *Run Configuration* for testing your project. Please ask a supervisor if you are unsure of how to set this up.  
 