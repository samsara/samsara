require_relative '../helper'

describe SamsaraSDK::Client do
  describe '#publish_events' do
    it 'goes smth' do
      client = SamsaraSDK::Client.new({})
      expect client.instance_of? Object
    end
  end
end
