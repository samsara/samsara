describe 'SamsaraSDK::RingBuffer' do
  describe '#initialize' do
    it 'creates internal storage with given capacity' do
      [0, 1, 3, 10, 5_000].each do |n|
        subject = SamsaraSDK::RingBuffer.new(n)
        expect(subject.instance_variable_get(:@buffer).size).to eq n
      end
    end

    it 'creates internal storage with initial counter and pointer' do
      subject = SamsaraSDK::RingBuffer.new(5)
      expect(subject.instance_variable_get(:@count)).to eq 0
      expect(subject.instance_variable_get(:@start)).to eq 0
    end
  end

  describe '#full?' do
    it 'returns true if buffer is full' do
      subject = SamsaraSDK::RingBuffer.new(2)
      subject << 'a'
      subject << 'b'
      expect(subject.full?).to be TRUE
    end

    it 'returns true if number of elements pushed to buffer are greater than buffer capacity' do
      subject = SamsaraSDK::RingBuffer.new(2)
      subject << 'a'
      subject << 'b'
      subject << 'c'
      expect(subject.full?).to be TRUE
    end

    it 'returns false if buffer is not full' do
      subject = SamsaraSDK::RingBuffer.new(2)
      subject << 'a'
      expect(subject.full?).to be FALSE
    end

    it 'returns false if buffer is empty' do
      subject = SamsaraSDK::RingBuffer.new(2)
      expect(subject.full?).to be FALSE
    end
  end

  describe '#empty?' do
    it 'returns true if buffer has zero elements' do
      subject = SamsaraSDK::RingBuffer.new(3)
      expect(subject.empty?).to be TRUE
    end

    it 'returns false if buffer is full' do
      subject = SamsaraSDK::RingBuffer.new(3)
      subject << 'a'
      subject << 'b'
      subject << 'c'
      subject << 'd'
      expect(subject.empty?).to be FALSE
    end

    it 'returns false if buffer has at least one element' do
      subject = SamsaraSDK::RingBuffer.new(3)
      subject << 'a'
      expect(subject.empty?).to be FALSE
    end

    it 'returns false if buffer has several elements' do
      subject = SamsaraSDK::RingBuffer.new(3)
      subject << 'a'
      subject << 'b'
      expect(subject.empty?).to be FALSE
    end
  end

  describe '#push' do
    subject { SamsaraSDK::RingBuffer.new(5) }

    it 'adds element' do
      subject << 'a'
      expect(subject.count).to eq 1
    end

    it 'has alias <<' do
      subject << 'a'
      expect(subject.count).to eq 1
    end

    it 'raises error if we try pushing to buffer with initial zero capacity' do
      subject = SamsaraSDK::RingBuffer.new(0)
      expect { subject << 'f' }.to raise_error(Exception)
    end

    it 'adds elements next to each other ' do
      subject << 'a'
      subject << 'b'
      subject << 'c'
      expect(subject.flush).to match_array %w(a b c)
    end

    it 'can take more elements than the initial capacity' do
      subject << 'a'
      subject << 'b'
      subject << 'c'
      subject << 'd'
      subject << 'e'
      subject << 'f'
      expect(subject.count).to eq 5
    end

    it 'overwrites elements than exceed the initial capacity' do
      subject << 'a'
      subject << 'b'
      subject << 'c'
      subject << 'd'
      subject << 'e'
      subject << 'f'
      expect(subject.flush).to match_array %w(f b c d e)
    end
  end

  describe '#flush' do
    it 'returns snapshot of only actual (not nil) elements' do
      (0...5).each do |n|
        subject = SamsaraSDK::RingBuffer.new(5)
        n.times { subject << 'a' }
        expect(subject.flush.size).to eq n
      end
    end

    it 'returns snapshot of actual elements in FIFO order' do
      skip 'rewrite!!!'
      [
        [[nil], 0, []],
        [[nil, nil, nil, nil], 0, []],
        [[nil, nil, nil, 1], 3, [1]],
        [[1], 0, [1]],
        [[1, 2], 1, [1, 2]],
        [[1, 2, 3], 2, [1, 2, 3]],
        [[4, 2, 3], 0, [2, 3, 4]],
        [[4, 5, 3], 1, [3, 4, 5]],
        [[1, 2, 3, nil, nil], 2, [1, 2, 3]],
        [[1, 2, 3, nil, nil], 0, [2, 3, 1]],
        [[nil, 1, 2, 3], 2, [3, 1, 2]],
        [[nil, nil, 1, 2, 3, nil], 2, [2, 3, 1]],
        [[nil, nil, 1, 2, 3, nil], 4, [1, 2, 3]]
      ].each do |data, position, result|
        subject = SamsaraSDK::RingBuffer.new(data.size)
        subject.instance_variable_set(:@buffer, data)
        subject.instance_variable_set(:@start, position)
        expect(subject.flush).to eq(result)
      end
    end

    it 'deletes data snapshot from itself if no block was given' do
      subject = SamsaraSDK::RingBuffer.new(5)
      subject << { foo: '123' }
      subject << { foo: '456' }
      subject.flush
      expect(subject.flush).to eq []
    end

    it 'passes data snapshot to a given block' do
      result = nil
      subject = SamsaraSDK::RingBuffer.new(5)
      subject << { foo: '123' }
      subject << { foo: '456' }
      subject.flush { |data| result = data }
      expect(result).to eq [{ foo: '123' }, { foo: '456' }]
    end

    it 'deletes snapshot data if given block processing succeeds' do
      subject = SamsaraSDK::RingBuffer.new(5)
      subject << { foo: '123' }
      subject << { foo: '456' }
      subject.flush { TRUE }
      expect(subject.flush).to eq []
    end

    it 'preserves snapshot data if given block processing did not succeed' do
      subject = SamsaraSDK::RingBuffer.new(5)
      subject << { foo: '123' }
      subject << { foo: '456' }
      subject.flush { FALSE }
      expect(subject.flush).to eq [{ foo: '123' }, { foo: '456' }]
    end
  end
end
