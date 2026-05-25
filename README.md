## About

Simple Raw text web server. It could be compatible with [Gopher protocol](https://ru.wikipedia.org/wiki/Gopher), but with some custom things.

Works also as simple HTTP server for hosting same data in http and gopher worlds.

It reads first line from request, gets request path and type (for HTTP). After that it returns requested data or default hello page.

## Build

```sh
javac src/*.java -d out
```

## Run

- with content from current dir

```sh
java -cp out App
```

- with content from external dir

```sh
java -cp out App external_dir_path
```
