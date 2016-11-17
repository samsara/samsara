require 'samsara_sdk/config'
require 'samsara_sdk/errors'

# Samsara SDK
module SamsaraSDK
  # Contains methods for event manipulation.
  class Event
    # Validation schema for Ingestion API event.
    @schema = {
      sourceId:  { required: TRUE, type: String },
      timestamp: { required: TRUE, type: Integer, min: 0 },
      eventName: { required: TRUE, type: String }
    }

    # Validation rules for Ingestion API event.
    @rules = {
      required: lambda do |key, value, required|
        raise EventValidationError "Event.#{key}: Argument is required" if required && value.nil?
      end,
      type: ->(key, value, type) { raise EventValidationError, "Event.#{key}: Wrong type" unless value.is_a? type },
      min: ->(key, value, min) { raise EventValidationError, "Event.#{key}: Can't be less than #{min}" if value < min }
    }

    class << self
      # Enriches missing event properties with ones from config.
      #
      # @param event [Hash] Event for Ingestion API.
      def enrich(event)
        event[:sourceId] = Config.get[:sourceId] if event[:sourceId].nil?
        event[:timestamp] = Config.timestamp if event[:timestamp].nil?
        event
      end

      # Validates event to conform Ingestion API requirements.
      #
      # @param event [Hash] Event.
      # @raise [SamsaraSDK::EventValidationError] if any option of the given event is invalid.
      def validate(event)
        @schema.each do |schema_param, schema_value|
          schema_value.each do |rule_key, rule_value|
            @rules[rule_key].call(schema_param, event[schema_param], rule_value)
          end
        end
      end
    end
  end
end
