## Samsara version scheme

Versions are set as follow:

    1.5.6.2
    | | | \-> patch release: contains only fixes
    | | \---> minor release: may contains fixes and little improvements
    | |                      all changes must be backward compatible
    | \-----> major release: loads of improvements and new features
    \-------> breaking release: major updates are made some of which might
                                break the backward compatibility.

**NOTE: that the leading 0 version (0.*.*.*) means that APIs not set
yet, so breaking changes might appear in lower releases.**
