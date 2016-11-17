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
      Config.setup! config
      @publisher = Publisher.new
      @queue = RingBuffer.new(Config.get[:max_buffer_size])
    end

    # Publishes given events list to Ingestion API immediately.
    #
    # @param events [Array<Hash>] List of events.
    # @return [Boolean] The result of puplish operation.
    # @raises 
    def publish_events(events)
      events = events.map do |event|
        Event.validate event
        Event.enrich event
      end
      @publisher.send events
    end

    # Pushes event to internal events' queue.
    #
    #
    #
    # todo - arg order
    def record_event(event_name, source_id = nil, timestamp = nil)
      event = {
        timestamp: timestamp,
        sourceId: source_id,
        eventName: event_name
      }
      event = Event.enrich event
      Event.validate event
      # @queue << event
    end
  end
end
