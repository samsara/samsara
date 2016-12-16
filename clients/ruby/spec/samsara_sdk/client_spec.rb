describe 'SamsaraSDK::Client' do
  context 'when initialized' do
    it 'should be given a url for Ingestion API' do
      expect { SamsaraSDK::Client.new({}) }.to raise_error(SamsaraSDK::ConfigValidationError, /URL/)
    end

    it 'can be given a hash of configuration overrides' do
      [
        { url: 'http://foo.bar' },
        { url: 'http://foo.bar', compression: :none },
        { sourceId: 'bar.baz', url: 'http://foo.bar' },
        { url: 'http://foo.bar', send_timeout_ms: 700 }
      ].each do |config|
        expect { SamsaraSDK::Client.new config }.not_to raise_error
      end
    end

    it 'should call Config.setup! with injected params' do
      config = { url: 'http://foo.bar', compression: :none }
      expect(SamsaraSDK::Config).to receive(:setup!).with(config)
      SamsaraSDK::Client.new config
    end

    it 'should instantiate a queue for events' do
      expect(SamsaraSDK::RingBuffer).to receive(:new).with(900)
      SamsaraSDK::Client.new(url: 'http://foo.bar', max_buffer_size: 900)
    end

    it 'should instantiate a publisher to push events to Ingestion API' do
      expect(SamsaraSDK::Publisher).to receive(:new)
      SamsaraSDK::Client.new(url: 'http://foo.bar')
    end

    it 'should start publishing thread if configured to do that' do
      expect(Thread).to receive(:new).and_yield
      expect_any_instance_of(SamsaraSDK::Client).to receive(:publishing_activity).once
      SamsaraSDK::Client.new(url: 'http://foo.bar', start_publishing_thread: TRUE)
    end

    it 'should not start publishing thread if not configured to do that' do
      expect(Thread).not_to receive(:new)
      expect_any_instance_of(SamsaraSDK::Client).not_to receive(:publishing_activity)
      SamsaraSDK::Client.new(url: 'http://foo.bar', start_publishing_thread: FALSE)
    end
  end

  context 'when instantiated' do
    subject { SamsaraSDK::Client.new(url: 'http://foo.bar') }

    it 'responds to #publish_events' do
      expect(subject).to respond_to(:publish_events)
    end
    it 'responds to #record_event' do
      expect(subject).to respond_to(:record_event)
    end
  end

  describe '#publish_events' do
    subject { SamsaraSDK::Client.new(url: 'http://foo.bar') }
    let(:events) do
      [
        { sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057 },
        { sourceId: 'foo2', eventName: 'baz2', timestamp: 1_479_988_964_057 },
        { sourceId: 'foo-bar-baz', eventName: 'test-test-test', timestamp: 1_479_988_964_857 }
      ]
    end
    before(:example) do
      allow_any_instance_of(SamsaraSDK::Publisher).to receive(:post)
    end

    it 'pushes given events to Ingestion API' do
      expect_any_instance_of(SamsaraSDK::Publisher).to receive(:post).with(events)
      subject.publish_events(events)
    end

    it 'does not take single event not wrapped in a list' do
      expect { subject.publish_events(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057) }
        .to raise_error(Exception)
    end

    it 'returns the result of Ingestion API POST operation' do
      [
        TRUE,
        FALSE
      ].each do |result|
        allow_any_instance_of(SamsaraSDK::Publisher).to receive(:post).and_return(result)
        expect(subject.publish_events(events)).to be(result)
      end
    end

    it 'enriches and validates each event from a given list' do
      expect(SamsaraSDK::Event).to receive(:enrich).with(1).and_return(1).once
      expect(SamsaraSDK::Event).to receive(:validate).with(1).and_return(1).once
      expect(SamsaraSDK::Event).to receive(:enrich).with(2).and_return(2).once
      expect(SamsaraSDK::Event).to receive(:validate).with(2).and_return(2).once
      subject.publish_events([1, 2])
    end

    context 'when any of the given events is invalid' do
      let(:invalid_events) do
        [
          { sourceId: 'test', timestamp: 1_479_988_864_057 },
          { sourceId: 'foo2', eventName: 'baz2', timestamp: 1_479_988_964_057 }
        ]
      end
      it 'raises SamsaraSDK::EventValidationError if any of the events is invalid' do
        expect { subject.publish_events(invalid_events) }.to raise_error(SamsaraSDK::EventValidationError)
      end

      it 'does not POST anything to Ingestion API' do
        expect_any_instance_of(SamsaraSDK::Publisher).not_to receive(:post)
        expect { subject.publish_events(invalid_events) }.to raise_error(SamsaraSDK::EventValidationError)
      end
    end
  end

  describe '#record_event' do
    subject { SamsaraSDK::Client.new(url: 'http://foo.bar') }
    let(:events) do
      [
        { sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057 },
        { sourceId: 'foo2', eventName: 'baz2', timestamp: 1_479_988_964_057 },
        { sourceId: 'foo-bar-baz', eventName: 'test-test-test', timestamp: 1_479_988_964_857 }
      ]
    end
    let(:event) { { sourceId: 'foo', eventName: 'single', timestamp: 1_479_888_824_057 } }

    it 'takes only single event' do
      expect { subject.record_event events }.to raise_error(Exception)
    end

    it 'adds event to a queue' do
      queue = double('Queue')
      expect(SamsaraSDK::RingBuffer).to receive(:new).and_return(queue)
      expect(queue).to receive(:<<).with(event).once
      subject.record_event event
    end

    it 'enriches and validates given event' do
      expect(SamsaraSDK::Event).to receive(:enrich).with(event).and_return(event).once
      expect(SamsaraSDK::Event).to receive(:validate).with(event).and_return(event).once
      subject.record_event(event)
    end
  end

  describe '#publishing_activity' do
    it 'flushes queue and posts flushed data if min_buffer_size threshold is reached' do
      config = {
        url: 'http://foo.bar',
        max_buffer_size: 3,
        min_buffer_size: 1,
        publish_interval_ms: 1,
        start_publishing_thread: FALSE
      }
      subject = SamsaraSDK::Client.new(config)
      expect(subject).to receive(:loop).and_yield
      subject.record_event(sourceId: 'foo', eventName: 'baz')
      expect(subject.instance_variable_get(:@queue)).to receive(:flush).and_yield('some_data').once
      expect(subject.instance_variable_get(:@publisher)).to receive(:post).with('some_data').once
      subject.send(:publishing_activity)
    end

    it 'does not flush queue and not post any data if min_buffer_size threshold is low' do
      config = {
        url: 'http://foo.bar',
        max_buffer_size: 3,
        min_buffer_size: 2,
        publish_interval_ms: 1,
        start_publishing_thread: FALSE
      }
      subject = SamsaraSDK::Client.new(config)
      expect(subject).to receive(:loop).and_yield
      subject.record_event(sourceId: 'foo', eventName: 'baz')
      expect(subject.instance_variable_get(:@queue)).not_to receive(:flush)
      expect(subject.instance_variable_get(:@publisher)).not_to receive(:post)
      subject.send(:publishing_activity)
    end

    it 'periodically suspends itself based on publish_interval_ms config value' do
      config = {
        url: 'http://foo.bar',
        publish_interval_ms: 5000,
        start_publishing_thread: FALSE
      }
      subject = SamsaraSDK::Client.new(config)
      expect(subject).to receive(:loop).and_yield
      expect(subject).to receive(:sleep).with(5).once
      subject.send(:publishing_activity)
    end
  end
end
