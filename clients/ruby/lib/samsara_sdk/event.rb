require 'samsara_sdk/config'
require 'samsara_sdk/errors'

# Samsara SDK
module SamsaraSDK
  # Contains methods for event manipulation.
  class Event
    # Validation schema for Ingestion API event.
    @schema = {
      sourceId:  { required: TRUE, not_blank: TRUE, type: String },
      timestamp: { required: TRUE, type: Integer, min: 0 },
      eventName: { required: TRUE, not_blank: TRUE, type: String }
    }

    # Validation rules for Ingestion API event.
    @rules = {
      required: lambda do |key, value, required|
        raise EventValidationError, "Event.#{key}: Argument is required" if required && value.nil?
      end,
      not_blank: lambda do |key, value, on|
        value.strip! if value.respond_to?(:strip!)
        raise EventValidationError, "Event.#{key}: can't be blank" if
            on && value.respond_to?(:empty?) ? value.empty? : !value
      end,
      type: ->(key, value, type) { raise EventValidationError, "Event.#{key}: Wrong type" unless value.is_a? type },
      min: ->(key, value, min) { raise EventValidationError, "Event.#{key}: Can't be less than #{min}" if value < min }
    }

    class << self
      # Enriches missing event properties with ones from config.
      #
      # @param event [Hash] Event for Ingestion API.
      # @return [Hash] Event for Ingestion API filled with missing properties.
      def enrich(event)
        event[:sourceId] = event.fetch(:sourceId, Config.get[:sourceId])
        event[:timestamp] = event.fetch(:timestamp, Config.timestamp)
        event
      end

      # Validates event to conform Ingestion API requirements.
      #
      # @param event [Hash] Event.
      # @return [Hash] Passed back event.
      # @raise [SamsaraSDK::EventValidationError] if any option of the given event is invalid.
      def validate(event)
        @schema.each do |schema_param, schema_value|
          schema_value.each do |rule_key, rule_value|
            @rules[rule_key].call(schema_param, event[schema_param], rule_value)
          end
        end
        event
      end
    end
  end
end
