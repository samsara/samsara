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
    # @raise [SamsaraSDK::ConfigValidationError] if any config option is incorrect.
    def initialize(config)
      Config.setup! config
      @publisher = Publisher.new
      @queue = RingBuffer.new(Config.get[:max_buffer_size])
    end

    # Publishes given events list to Ingestion API immediately.
    #
    # @param events [Array<Hash>] List of events.
    # @return [Boolean] The result of puplish operation.
    # @see http://samsara-analytics.io/docs/design/events-spec Event specification
    # @raise [SamsaraSDK::EventValidationError] if any option of the given event is invalid.
    def publish_events(events)
      events = events.map do |event|
        Event.validate event
        Event.enrich event
      end
      @publisher.post events
    end

    # Pushes event to internal events' queue.
    #
    # @param data [Hash] Event data.
    # @option data [String] :eventName Name of the event.
    # @option data [String] :sourceId Source ID of the event.
    # @option data [Integer] :timestamp Timestamp in milliseconds.
    # @see http://samsara-analytics.io/docs/design/events-spec Event specification
    def record_event(event)
      event = Event.enrich event
      Event.validate event
      # @queue << event
    end
  end
end
