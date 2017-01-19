describe 'SamsaraSDK::Event' do
  subject { SamsaraSDK::Event }

  describe '.enrich' do
    it 'populates event with missing attributes' do
      [
        { eventName: 'foo' },
        { eventName: 'foo', timestamp: 123 },
        { eventName: 'foo', sourceId: 'baz', timestamp: 123 }
      ].each do |event|
        result = subject.enrich event
        expect(result).to have_key(:sourceId)
        expect(result).to have_key(:timestamp)
        expect(result).to have_key(:eventName)
      end
    end

    it 'preserves existing attribute values' do
      result = subject.enrich(eventName: 'foo', sourceId: 'baz', timestamp: 123)
      expect(result[:eventName]).to eql('foo')
      expect(result[:sourceId]).to eql('baz')
      expect(result[:timestamp]).to eql(123)
    end

    it 'preserves existing additional attribute values' do
      result = subject.enrich(eventName: 'foo', sourceId: 'baz', timestamp: 123, color: 'red', level: 80)
      expect(result[:eventName]).to eql('foo')
      expect(result[:sourceId]).to eql('baz')
      expect(result[:timestamp]).to eql(123)
      expect(result[:color]).to eql('red')
      expect(result[:level]).to eql(80)
    end

    it 'populates event with correct timestamp' do
      result = subject.enrich(eventName: 'foo')
      expect(result[:timestamp].to_s.size).to eq(13)
    end

    it 'populates event with correct sourceId' do
      SamsaraSDK::Config.setup!(url: 'http://test', sourceId: 'baz.bar')
      result = subject.enrich(eventName: 'foo')
      expect(result[:sourceId]).to eql('baz.bar')
    end
  end

  describe '.validate' do
    it 'returns given event' do
      event = {
        sourceId: 'foo',
        eventName: 'bar',
        timestamp: 1_479_988_864_057
      }
      result = subject.validate event
      expect(result).to equal(event)
    end

    it 'returns given event with preserved additional values' do
      event = {
        sourceId: 'foo',
        eventName: 'bar',
        timestamp: 1_479_988_864_057,
        color: 'red',
        level: 10
      }
      result = subject.validate event
      expect(result).to equal(event)
    end

    context 'when given invalid event' do
      it 'raises SamsaraSDK::EventValidationError with invalidation message' do
        [
          [{}, 'sourceId.*required'],
          [{ sourceId: 'foo' }, 'timestamp.*required'],
          [{ sourceId: 'foo', timestamp: 1_479_988_864_057 }, 'eventName.*required'],
          [{ sourceId: '', eventName: 'bar', timestamp: 1_479_988_864_057 }, 'sourceId.*blank'],
          [{ sourceId: ' ', eventName: 'bar', timestamp: 1_479_988_864_057 }, 'sourceId.*blank'],
          [{ sourceId: 'foo', eventName: '', timestamp: 1_479_988_864_057 }, 'eventName.*blank'],
          [{ sourceId: 'foo', eventName: '   ', timestamp: 1_479_988_864_057 }, 'eventName.*blank'],
          [{ sourceId: 123, eventName: 'bar', timestamp: 1_479_988_864_057 }, 'sourceId.*type'],
          [{ sourceId: ['foo'], eventName: 'bar', timestamp: 1_479_988_864_057 }, 'sourceId.*type'],
          [{ sourceId: { foo: 'baz' }, eventName: 'bar', timestamp: 1_479_988_864_057 }, 'sourceId.*type'],
          [{ sourceId: 'foo', eventName: ['foo'], timestamp: 1_479_988_864_057 }, 'eventName.*type'],
          [{ sourceId: 'foo', eventName: { foo: 'baz' }, timestamp: 1_479_988_864_057 }, 'eventName.*type'],
          [{ sourceId: 'foo', eventName: { foo: 'baz' }, timestamp: -1 }, 'timestamp.*less']
        ].each do |event, message|
          expect { subject.validate(event) }.to raise_error(SamsaraSDK::EventValidationError, /#{message}/)
        end
      end
    end

    context 'when given valid event' do
      it 'does not raise errors' do
        [
          { sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057 },
          { sourceId: 'foo', eventName: 'baz', timestamp: 0 },
          { sourceId: 'foo', eventName: 'baz', timestamp: 15 },
          { sourceId: 'foo.bar', eventName: 'baz', timestamp: 1_479_988_864_057 },
          { sourceId: '  foo  ', eventName: 'baz.test.test.test.test', timestamp: 1_479_988_864_057 }
        ].each do |event|
          expect { subject.validate(event) }.not_to raise_error
        end
      end
    end
  end
end
