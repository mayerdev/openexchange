# OpenExchange

Is a free and open source crypto-currency exchange core.

Main repository: https://gitlab.com/mayerdev/openexchange

## Requirements

- PostgreSQL 17.x with TimescaleDB
- NATS 2.12.5
- Redis 7.x

## Structure

| Folder        | Description            |
|---------------|------------------------|
| matching-core | Order matching engine  |
| auth          | Authentication service |

## Features

The list of features will be updated as development progresses.

**matching-core**:

- [x] Order types
    - [x] Limit
    - [x] Market
- [x] Order matching engine
- [x] Trading history
- [x] Basic order book
- [x] Candlestick charts

**auth**:
