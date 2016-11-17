describe SamsaraSDK::Config do
  describe '.defaults' do
    it 'returns default config values as a Hash' do
      expect(SamsaraSDK::Config.defaults).to be(Hash)
    end

    it 'returns default config without :url specified value' do
      expect(SamsaraSDK::Config.defaults[:url]).to eq(nil)
    end

    it 'returns default config with :start_publishing_thread set to true' do
      expect(SamsaraSDK::Config.defaults[:start_publishing_thread]).to eq(TRUE)
    end

    it 'returns default config with :compression set to gzip' do
      expect(SamsaraSDK::Config.defaults[:compression]).to eq('gzip')
    end
  end

  describe '.sanitize' do
    it 'raises exception if no value was specified for :url' do
      expect(SamsaraSDK::Config.defaults).to be(Hash)
    end
  end
end
