describe 'SamsaraSDK::Config' do
  subject { SamsaraSDK::Config }

  before(:example) do
    subject.setup!(url: 'foo', sourceId: 'bar', min_buffer_size: 10)
  end

  describe 'class constants check' do
    it 'PUBLISHED_TIMESTAMP_HEADER is correct' do
      expect(subject::PUBLISHED_TIMESTAMP_HEADER).to eql('X-Samsara-publishedTimestamp')
    end

    it 'API_PATH is correct' do
      expect(subject::API_PATH).to eql('/v1/events')
    end
  end

  describe '.setup!' do
    it 'overrides default config values' do
      subject.setup!(url: 'foo', sourceId: 'baz', min_buffer_size: 55)
      expect(subject.get[:url]).to eq('foo')
      expect(subject.get[:sourceId]).to eq('baz')
      expect(subject.get[:min_buffer_size]).to eq(55)
    end

    context 'when given invalid configuration' do
      it 'raises SamsaraSDK::ConfigValidationError' do
        [
          [{ start_publishing_thread: 0 }, 'start_publishing_thread should be'],
          [{ publish_interval_ms: 1.2 }, 'publish_interval_ms should be'],
          [{ publish_interval_ms: '30_000' }, 'publish_interval_ms should be'],
          [{ max_buffer_size: 1.2 }, 'max_buffer_size should be'],
          [{ max_buffer_size: '30_000' }, 'max_buffer_size should be'],
          [{ min_buffer_size: 1.6 }, 'min_buffer_size should be'],
          [{ min_buffer_size: '30' }, 'min_buffer_size should be'],
          [{ send_timeout_ms: 0.8 }, 'send_timeout_ms should be'],
          [{ send_timeout_ms: '789' }, 'send_timeout_ms should be'],
          [{ compression: 'gzip' }, 'compression should be'],
          [{ compression: TRUE }, 'compression should be'],
          [{ url: 123 }, 'url should be'],
          [{ url: nil }, 'url should be'],
          [{ url: [1, 2, 3] }, 'url should be'],
          [{ url: { a: 'http://foo.bar' } }, 'url should be'],
          [{ url: '' }, 'URL.*should be specified.'],
          [{ publish_interval_ms: 0 }, 'Invalid interval'],
          [{ publish_interval_ms: -2 }, 'Invalid interval'],
          [{ compression: :zip }, 'Incorrect compression'],
          [{ max_buffer_size: 4, min_buffer_size: 8 }, 'be less than']
        ].each do |input, message|
          data = { url: 'foo' }.merge input
          expect { subject.setup!(data) }.to raise_error(SamsaraSDK::ConfigValidationError, /#{message}/)
        end
      end
    end

    context 'when given valid configuration' do
      it 'raises no errors' do
        [
          { start_publishing_thread: FALSE },
          { sourceId: 'some-source-id' },
          { publish_interval_ms: 20 },
          { send_timeout_ms: 10 },
          { compression: :gzip },
          { compression: :none },
          { url: 'http://foo.bar' },
          { publish_interval_ms: 14 },
          { max_buffer_size: 8, min_buffer_size: 4 }
        ].each do |input|
          data = { url: 'foo' }.merge input
          expect { subject.setup!(data) }.not_to raise_error
        end
      end
    end
  end

  describe '.timestamp' do
    it 'returns actual timestamp' do
      now = subject.timestamp
      sleep 0.001
      later = subject.timestamp
      expect(now).to be < later
    end

    it 'returns integer timestamp' do
      expect(subject.timestamp).to be_an(Integer)
    end

    it 'returns timestamp in milliseconds' do
      expect(subject.timestamp.to_s.size).to eql(13)
    end
  end

  describe '.get' do
    context 'when setup! was done' do
      it 'returns non-empty Hash' do
        expect(subject.get).not_to eq({})
      end

      it 'returns Hash with config values' do
        expect(subject.get[:url]).not_to eq(nil)
        expect(subject.get[:sourceId]).not_to eq(nil)
        expect(subject.get[:start_publishing_thread]).not_to eq(nil)
        expect(subject.get[:publish_interval_ms]).not_to eq(nil)
        expect(subject.get[:max_buffer_size]).not_to eq(nil)
        expect(subject.get[:min_buffer_size]).not_to eq(nil)
        expect(subject.get[:send_timeout_ms]).not_to eq(nil)
        expect(subject.get[:compression]).not_to eq(nil)
      end
    end
  end
end
