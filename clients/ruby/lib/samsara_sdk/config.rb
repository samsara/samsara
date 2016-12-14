require 'samsara_sdk/errors'

# Samsara SDK
module SamsaraSDK
  # Samsara SDK default configuration and request constants.
  class Config
    # Samsara specific HTTP Header
    PUBLISHED_TIMESTAMP_HEADER = 'X-Samsara-publishedTimestamp'.freeze

    # Samsara Ingestion API endpoint
    API_PATH = '/v1/events'.freeze

    # Default configuration values.
    @defaults = {
      # Samsara ingestion api endpoint "http://samsara-ingestion.local/"
      url: '',

      # Identifier of the source of these events
      # OPTIONAL used only for record-event
      sourceId: '',

      # Start the publishing thread?
      start_publishing_thread: TRUE,

      # How often should the events being sent to Samsara
      # in milliseconds.
      # default = 30s
      publish_interval_ms: 30_000,

      # Max size of the buffer.
      # When buffer is full older events are dropped.
      max_buffer_size: 10_000,

      # Minimum number of events that must be in the buffer
      # before attempting to publish them.
      min_buffer_size: 100,

      # Network timeout for send operations
      # in milliseconds.
      # default 30s
      send_timeout_ms: 30_000,

      # Should the payload be compressed?
      # allowed values :gzip, :none
      compression: :gzip,

      # NOT CURRENTLY SUPPORTED
      # Add Samsara client statistics events
      # this helps you to understand whether the
      # buffer size and publish-intervals are
      # adequately configured.
      # send_client_stats: TRUE
    }

    # Actual configuration values.
    @values = {}

    class << self
      # Set up configuration.
      # Merges given config with defaults and validates the result.
      #
      # @param input_config [Hash] Input configuration options.
      # @raise [SamsaraSDK::ConfigValidationError] if any config option is incorrect.
      def setup!(input_config)
        @values = @defaults.merge input_config
        validate @values
      end

      # Get configuration values.
      #
      # @return [Hash] Configuration values.
      def get
        @values
      end

      # Generates current timestamp.
      #
      # @return [Integer] timestamp in milliseconds.
      def timestamp
        (Time.now.to_f.round(3) * 1000).to_i
      end

      private

      # Validates given configuration values.
      #
      # @param config [Hash] Configuration values.
      # @raise [SamsaraSDK::ConfigValidationError] if any config option is incorrect.
      def validate(config)
        config.each do |k, v|
          raise ConfigValidationError, "#{k} should be of #{@defaults[k].class}" unless valid_type?(k, v)
        end

        raise ConfigValidationError, 'URL for Ingestion API should be specified.' if config[:url] == ''
        raise ConfigValidationError, 'Incorrect compression option.' unless [:gzip, :none].include? config[:compression]
        raise ConfigValidationError, 'Invalid interval time for Samsara client.' if config[:publish_interval_ms] <= 0
        raise ConfigValidationError, 'max_buffer_size can not be less than min_buffer_size.' if
            config[:max_buffer_size] < config[:min_buffer_size]
      end

      # Check value type corresponds to allowed type for particular config key.
      #
      # @param key [Symbol] Configuration key.
      # @param value [Object] Configuration value.
      # @return [Boolean]
      def valid_type?(key, value)
        if [TrueClass, FalseClass].include? @defaults[key].class
          return [TrueClass, FalseClass].include? value.class
        end
        value.is_a? @defaults[key].class
      end
    end
  end
end
