# Trading Data Import Service

A Spring Boot application for importing, processing, and aggregating cryptocurrency trading data from various sources, specifically designed to work with CryptoDataDownload (CDD) CSV files and convert them into TA4J time series for technical analysis.

## Features

### Data Sources
- **CryptoDataDownload Integration**: Fetch historical minute-level OHLCV data from CryptoDataDownload.com
- **Local File Processing**: Import data from local CSV files or directories
- **URL-based Import**: Download and process CSV files from direct URLs
- **Classpath Resources**: Load sample data from application resources

### Data Processing
- **Time Series Aggregation**: Convert 1-minute bars into higher timeframes (4m, 24m, etc.)
- **Data Validation**: Automatic filtering of malformed rows with non-finite values
- **Deduplication**: Remove duplicate entries based on timestamps
- **TA4J Integration**: Convert raw data into TA4J BarSeries for technical analysis

### API Endpoints
- **REST API**: Multiple endpoints for fetching and processing trading data
- **Flexible Parameters**: Support for different exchanges, symbols, and timeframes
- **JSON Response**: Returns series metadata including bar count, last close price, and timestamps

## Technology Stack

- **Java 21**: Modern Java features and performance
- **Spring Boot 3.5.5**: Enterprise-grade application framework
- **TA4J 0.18**: Technical analysis library for time series processing
- **Gradle**: Build automation and dependency management

## Project Structure

```
src/
├── main/
│   ├── java/com/example/importData/
│   │   ├── ImportDataApplication.java          # Main Spring Boot application
│   │   ├── CddClient.java                      # HTTP client for CryptoDataDownload
│   │   ├── CddBarSeriesService.java            # Core service for data processing
│   │   ├── Aggregations.java                   # Time series aggregation logic
│   │   ├── dto/
│   │   │   └── BarDto.java                     # Data transfer object for OHLCV bars
│   │   ├── config/
│   │   │   └── CddProperties.java              # Configuration properties
│   │   └── web/
│   │       └── CddBarsController.java          # REST API endpoints
│   └── resources/
│       ├── application.yaml                    # Application configuration
│       └── sample/                             # Sample CSV data files
└── test/
    └── java/com/example/importData/cdd/        # Unit tests
```

## Getting Started

### Prerequisites
- Java 21 or higher
- Gradle (wrapper included)

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd importData
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

### Configuration

Edit `src/main/resources/application.yaml`:

```yaml
server:
  port: 8080

cdd:
  base-url: https://www.cryptodatadownload.com
  default-exchange: Binance
```

## API Documentation

### Fetch Data from CryptoDataDownload

```http
GET /cdd/bars?symbol=BTCUSDT&exchange=Binance&tf=4m
```

**Parameters:**
- `symbol` (required): Trading pair symbol (e.g., BTCUSDT)
- `exchange` (optional): Exchange name (default: Binance)
- `tf` (optional): Timeframe (default: 4m)

### Fetch Data from URL

```http
GET /cdd/bars/url?csvUrl=https://example.com/data.csv&name=BTCUSDT_4m&tf=4m
```

**Parameters:**
- `csvUrl` (required): Direct URL to CSV file
- `name` (required): Series name
- `tf` (optional): Timeframe (default: 4m)

### Quick Start Endpoints

```http
GET /cdd/bars/4m?symbol=BTCUSDT&exchange=Binance
GET /cdd/bars/24m?symbol=BTCUSDT&exchange=Binance
GET /cdd/bars/url/4m?csvUrl=URL&name=SeriesName
GET /cdd/bars/url/24m?csvUrl=URL&name=SeriesName
```

### Sample Response

```json
{
  "seriesName": "BTCUSDT-4m-cdd",
  "barCount": 150,
  "lastClose": "65070.0",
  "lastTime": "2024-08-23T00:07:00Z"
}
```

## Data Format

### Input CSV Format
The application expects CSV files with the following columns:
- `unix` or `timestamp`: Unix timestamp
- `date`: Human-readable date (optional)
- `symbol`: Trading pair symbol
- `open`: Opening price
- `high`: Highest price
- `low`: Lowest price
- `close`: Closing price
- `volume` or `Volume BTC`: Trading volume

### Sample CSV Data
```csv
# Cryptocurrency data from CryptoDataDownload
unix,timestamp,date,symbol,open,high,low,close,Volume BTC,Volume USDT,tradecount
1724371200,1724371200,2024-08-23 00:00:00,BTCUSDT,65000,65050,64980,65020,12.345,802000,1200
1724371260,1724371260,2024-08-23 00:01:00,BTCUSDT,65020,65040,64990,65010,10.111,657000,980
```

## Data Processing Features

### Time Aggregation
- Converts 1-minute bars into any N-minute timeframe
- Properly calculates OHLC values:
  - **Open**: First bar's open price in the period
  - **High**: Maximum high price in the period
  - **Low**: Minimum low price in the period
  - **Close**: Last bar's close price in the period
  - **Volume**: Sum of all volumes in the period

### Data Quality
- **Validation**: Filters out rows with infinite or NaN values
- **Deduplication**: Removes duplicate timestamps (last entry wins)
- **Sorting**: Ensures chronological order
- **Flexible Parsing**: Handles various CSV formats and column names

## Testing

Run the test suite:

```bash
./gradlew test
```

Tests cover:
- Data aggregation logic
- CSV parsing functionality
- HTTP endpoint behavior
- Error handling scenarios

## Development

### Adding New Data Sources
1. Implement data fetching in `CddClient.java`
2. Add corresponding methods in `CddBarSeriesService.java`
3. Create new REST endpoints in `CddBarsController.java`
4. Add unit tests

### Customizing Aggregation
Modify `Aggregations.java` to implement custom aggregation logic or support additional timeframes.

## License

This project is licensed under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues and questions:
- Check the test files for usage examples
- Review the API endpoints documentation
- Examine the sample CSV data format
