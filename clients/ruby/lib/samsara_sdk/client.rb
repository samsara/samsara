require 'samsara_sdk/config'
require 'samsara_sdk/publisher'
require 'samsara_sdk/ring_buffer'
require 'samsara_sdk/event'

# Samsara SDK
module SamsaraSDK
  # A client for ingesting events into Samsara.
  # It is the main interface to communicate with Samsara API.
  class Client
    # initialize
    #
    # @param config [Hash] Configuration overrides.
    # @raise [ArgumentError] if any config option is incorrect.
    def initialize(config)
      @options = Config.defaults.merge config
      Config.validate @options

      @publisher = Publisher.new @options
      @queue = RingBuffer.new @options[:max_buffer_size]
    end

    # Publishes given events to Ingestion API immediately.
    def publish_events(events)
      Event.validate events
      @publisher.send events
    end

    def record_event(event)
    end
  end
end
