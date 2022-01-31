# edu-sharing community deploy
===========================

The edu-sharing open-source project started in 2007 to develop networked E-Learning environments for managing and
sharing educational contents inter-organisationally.

Documentation
-------------
More information can be found on the [homepage](http://www.edu-sharing.com).

Or visit the edu-sharing [documentation](http://docs.edu-sharing.com/confluence/edp).

Where can I get the latest release?
-----------------------------------
You can download source and binaries from
our [artifact repository](https://artifacts.edu-sharing.com).

Contributing
------------
For contribution on a regular basis please visit our [community site](http://edu-sharing-network.org/?lang=en).

Security Issues
---------------
If you found something which might could be a vulnerability or a security issue, please contact us first instead of
making a public issue. This can help us tracking down the issue first and may provide patches beforehand.

Please provide such concerns via mail to security@edu-sharing.com

Thanks!

Windows as a host system
------------------------
By default, the line endings under Windows are in CRLF format, they are not compatible with linux distributions that
uses LF the format. In order to enforce LF as the standard line end format, we have explicitly defined the format for
the respective files in .gitattributes file.

In addition to this configuration, Windows users must specify the following git settings from the command line:

In the git command shell, go to the project directory and set core.eol to lf and core.autocrlf to input.

```
cd <Path to the project root>
git config core.eol lf
git config core.autocrlf input
```

Finally, perform the checkout-index command to enforce the end-of-line format.

```
git checkout-index --force --all
```


