# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Jwt token scope verification based on swgger spec in security
- Status module to standardize error response

### Changed
- Update the jwt.json and secuirty.json to support multiple certificates and kid to pick up the right certificate
for jwt verification. Also, expired token will throw ExpiredJwtException now.


## 0.1.1 - 2016-09-19
### Added
- Audit
- Client
- Info
- Mask
- Validator



## 0.1.0 - 2016-08-16
### Added
- Server
- Config
- Utilit
- Security






