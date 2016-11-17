require 'json'
require 'zlib'
require 'stringio'
require 'uri'
require 'net/http'
require 'config'

# Samsara SDK
module SamsaraSDK
  # Physical connector that Publishes messages to Samsara Ingestion API.
  class Publisher
    # Sends message to Ingestion API.
    #
    # @param data [Array<Hash>] List of events.
    # @return [Boolean] Success or falure of HTTP POST call.
    def send(data)
      uri = URI.parse(Config.get[:url])
      https = Net::HTTP.new(uri.host, uri.port)
      https.use_ssl = true

      request = Net::HTTP::Post.new(prepare(data))
      request = request.merge generate_headers

      https.request(request)
    end

    private

    # Wraps data in JSON and optionally compresses it.
    #
    # @param data [Array<Hash>] Data to prepare.
    # @return [String] Prepared data.
    def prepare(data)
      data = JSON.generate data
      data = self.send(Config.get[:compression], data)
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
      data = wio.string
    end

    # None wrapper for data.
    #
    # @param data [String] Data to wrap.
    # @return [String] Original data.
    def none(data)
      data
    end

    # Helper method to generate HTTP request headers for Ingestion API.
    # @return [Hash] headers
    def generate_headers
      {
        'Accept' => 'application/json',
        'Content-Type' => 'application/json',
        'Content-Encoding' => Config.get[:compression] || 'identity',
        Config.PUBLISHED_TIMESTAMP_HEADER => Config.timestamp.to_s
      }
    end
  end
end
