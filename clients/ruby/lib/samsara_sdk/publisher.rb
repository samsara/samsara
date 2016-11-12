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
    # initialize.
    # @param options [Hash] Samsara SDK options.
    def initialize(options)
      @options = options
    end

    # Sends message to Ingestion API.
    # @param data [???]
    # @return [???]
    def send(data)
      uri = URI.parse(@host)
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
      if @options[:compression] == :gzip
        wio = StringIO.new('w')
        w_gz = Zlib::GzipWriter.new(wio)
        w_gz.write(data)
        w_gz.close
        data = wio.string
      end
      data
    end

    # Helper method to generate HTTP request headers for Ingestion API.
    # @return [Hash] headers
    def generate_headers
      {
        'Accept' => 'application/json',
        'Content-Type' => 'application/json',
        'Content-Encoding' => @options['compression'] || 'identity',
        @options.PUBLISHED_TIMESTAMP_HEADER => generate_timestamp.to_s
      }
    end

    # Helper method to generate current timestamp.
    #
    # @return [Integer] timestamp in milliseconds
    def generate_timestamp
      (Time.now.to_f.round(3) * 1000).to_i
    end
  end
end
