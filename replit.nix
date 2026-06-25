{pkgs}: {
  deps = [
    pkgs.python312Packages.rpds-py
    pkgs.python312Packages.flasgger
    pkgs.python312Packages.brotlicffi
    pkgs.python312Packages.brotli
    pkgs.python312Packages.python-dotenv
    pkgs.python312Packages.bcrypt
    pkgs.python312Packages.pyjwt
    pkgs.python312Packages.beautifulsoup4
    pkgs.python312Packages.requests
    pkgs.python312Packages.flask-limiter
    pkgs.python312Packages.flask-compress
    pkgs.python312Packages.flask-cors
    pkgs.python312Packages.flask
    pkgs.python312Packages.gunicorn
    pkgs.unzip
  ];
}
