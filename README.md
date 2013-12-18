JNapios - Java Nagios API
=======

Inspired by the https://github.com/xb95/nagios-api, JNapios gives a rapid approach to access the hosts and it's services of the servers monitored by a Nagios server.

It's only job is to provide a REST api to access the contents in status.dat of the Nagios server. It provides the content in JSON.

Usage
======

Build the project with gradle.

Then in the terminal:

./JNapios port=9001 statusFile=/path/to/nagios/status.dat logFolder=/path/to/log/folder
