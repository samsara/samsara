module SamsaraSDK
  # Samsara SDK default configuration and request constants.
  module Config
    # Samsara specific HTTP Header
    PUBLISHED_TIMESTAMP_HEADER = 'X-Samsara-publishedTimestamp'

    # Samsara API endpoint
    API_PATH = '/v1/events'

    # Default configuration values
    DEFAULT = {
      # Samsara ingestion api endpoint "http://samsara.io/"
      :url => nil,

      # Identifier of the source of these events
      # OPTIONAL used only for record-event
      :source_id => nil,

      # Start the publishing thread?
      :start_publishing_thread => TRUE,

      # How often should the events being sent to Samsara
      # in milliseconds
      # default = 30s
      :publish_interval_ms => 30000,

      # Max size of the buffer, when buffer is full
      # older events are dropped
      :max_buffer_size => 10000,

      # Minimum number of events to that must be in the buffer
      # before attempting to publish them
      :min_buffer_size => 100,

      # Network timeout for send operations
      # in milliseconds
      # default 30s
      :send_timeout_ms => 30000,

      # Should the payload be compressed?
      # allowed values 'gzip', 'none'
      :compression => 'gzip',

      # NOT CURRENTLY SUPPORTED
      # Add Samsara client statistics events
      # this helps you to understand whether the
      # buffer size and publish-intervals are
      # adequately configured.
      # :send_client_stats => TRUE
    }
  end
end
