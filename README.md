# Program AB

Program AB is the reference implementation of the AIML 2.0 draft specification.
AIML is a widely adopted standard for creating chat bots and mobile virtual
assistants like ALICE, Mitsuku, English Tutor, The Professor, S.U.P.E.R. and
many more. Program AB was developed by Richard Wallace (contact
info@alicebot.org) and first released in January, 2013. Following in the
tradition of naming AIML interpreters after letters of the alphabet (Program B,
Program D, Program N, Program O etc.), the name "AB" is intended to suggest a
fresh start with AIML 2.0. Program AB is an experimental platform for the
development of new features and serves as the reference implementation.

## This project

This project is aimed at understanding Program AB, by refactoring it in ways I
understand. Among other things:

- migration to Java 8, and building using Gradle
- separation of data file parsing and processing
- better encapsulation of data
