<!--
SPDX-FileCopyrightText: 2022 Erik Billing <erik.billing@his.se>

SPDX-License-Identifier: GPL-3.0-or-later
-->

# LuckyCardTester
Application tester for the LuckyCard assignment (IT401G, University of Skövde).

This utility allow you to check your solution to the LuckyCard assignment before you submit. This tool is intended to help you verify that your solution meets some basic requirements, but it does not check _all_ requirements and the test result should thus not be read as a guarantee for a pass.

## Usage: 
This utility has a text based interface meant to be used in a terminal. LuckyCardTester will compile and execute your project, any print the test result. 

`java LCTest.java`   _Prints help message._

`java LCTest.java .`   _Puns test in current folder._

`java LCTest.java <path to project folder>`   _Runs test for specified folder._