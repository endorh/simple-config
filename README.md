# Simple Config

Forge library mod to define config files with config GUIs in a simple manner.

Translation keys are automatically assigned to config entries by name.

There are two supported ways to declare configs. Using entry builders is recommended,
but there's also support for reflection based config generation from static config classes.

### Examples
The mod includes example configs, declared using both ways.

Check out the `endorh.simpleconfig.demo` package

### Contributing

If you intend to contribute to the grammars, read first the comment in the
`antlr.gradle` file, which explains the grammar source generation process.

In particular, do not use the `src/main/antlr` directory, since it's removed
after each generation.
