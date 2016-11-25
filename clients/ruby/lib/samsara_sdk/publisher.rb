require 'json'
require 'zlib'
require 'stringio'
require 'net/http'
require 'samsara_sdk/config'

# Samsara SDK
module SamsaraSDK
  # Physical connector that Publishes messages to Samsara Ingestion API.
  class Publisher
    # Sends message to Ingestion API.
    #
    # @param data [Array<Hash>] List of events.
    # @return [Boolean] Success or failure of HTTP POST call.
    def post(data)
      url = URI.parse(Config.get[:url].chomp('/') + Config::API_PATH)
      http = Net::HTTP.new(url.host, url.port)
      http.use_ssl = url.scheme == 'https'
      http.read_timeout = Config.get[:send_timeout_ms] / 1000
      request = Net::HTTP::Post.new(url, headers)
      request.body = prepare(data)
      http.request(request).instance_of? Net::HTTPAccepted
    rescue RuntimeError
      FALSE
    end

    private

    # Wraps data in JSON and optionally compresses it.
    #
    # @param data [Array<Hash>] Data to prepare.
    # @return [String] Prepared data.
    def prepare(data)
      data = JSON.generate data
      send(Config.get[:compression], data)
    end

    # Gzip wrapper for data.
    #
    # @param data [String] Data to wrap.
    # @return [String] Gzipped data.
    def gzip(data)
      wio = StringIO.new('w')
      w_gz = Zlib::GzipWriter.new(wio)
      w_gz.write(data)
      w_gz.close
      wio.string
    end

    # None wrapper for data.
    #
    # @param data [String] Data to wrap.
    # @return [String] Original data.
    def none(data)
      data
    end

    # Helper method to generate HTTP request headers for Ingestion API.
    #
    # @return [Hash] headers
    def headers
      {
        'Accept' => 'application/json',
        'Content-Type' => 'application/json',
        'Content-Encoding' => Config.get[:compression] == :gzip ? 'gzip' : 'identity',
        Config::PUBLISHED_TIMESTAMP_HEADER => Config.timestamp.to_s
      }
    end
  end
end
