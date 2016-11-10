require 'samsara_sdk/config'

module SamsaraSDK
  # A client for ingesting events into Samsara.
  # It is the main interface to communicate with Samsara API.
  class Client
    # initialize
    # @param config [Hash] Configuration overrides.
    def initialize(config)
      # config = Config.sanitize config
      @options = Config.defaults.merge config
    end

    def publish_events
    end

    def record_event
    end
  end
end
