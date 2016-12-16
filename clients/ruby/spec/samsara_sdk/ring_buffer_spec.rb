describe 'SamsaraSDK::RingBuffer' do
  describe '#initialize' do
    it 'creates internal storage with given capacity' do
      [0, 1, 3, 10, 5_000].each do |n|
        subject = SamsaraSDK::RingBuffer.new(n)
        expect(subject.instance_variable_get(:@buffer).size).to eq n
      end
    end

    it 'creates empty internal storage' do
      subject = SamsaraSDK::RingBuffer.new(5)
      expect(subject.full?).to be FALSE
      expect(subject.empty?).to be TRUE
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

    it 'does nothing if we try pushing to buffer with initial zero capacity' do
      subject = SamsaraSDK::RingBuffer.new(0)
      subject << 'a'
      expect(subject.count).to eq 0
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

    it 'overwrites elements that exceed the initial capacity' do
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
      [
        [[nil], 0, []],
        [[nil, nil, nil, nil], 0, []],
        [[nil, nil, nil, 1], 3, [1]],
        [[1], 0, [1]],
        [[1, 2], 1, [1, 2]],
        [[1, 2], 0, [2, 1]],
        [[1, 2, 3], 2, [1, 2, 3]],
        [[4, 2, 3], 0, [2, 3, 4]],
        [[4, 5, 3], 1, [3, 4, 5]],
        [[1, 2, 3, nil, nil], 2, [1, 2, 3]],
        [[1, 2, 3, nil, nil], 0, [2, 3, 1]],
        [[nil, 1, 2, 3], 2, [3, 1, 2]],
        [[nil, nil, 1, 2, 3, nil], 2, [2, 3, 1]],
        [[nil, nil, 1, 2, 3, nil], 4, [1, 2, 3]],
        [[80, nil, nil, 4, 5], 0, [4, 5, 80]],
        [[80, 99, nil, 4, 5], 1, [4, 5, 80, 99]]
      ].each do |data, position, result|
        subject = SamsaraSDK::RingBuffer.new(data.size)
        subject.instance_variable_set(:@buffer, data)
        subject.instance_variable_set(:@pointer, position)
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

    it 'preserves buffer storage capacity after data deletion' do
      subject = SamsaraSDK::RingBuffer.new(15)
      subject << { foo: '123' }
      subject << { foo: '456' }
      subject.flush { TRUE }
      expect(subject.instance_variable_get(:@buffer).size).to eq 15
      subject.flush { TRUE }
      expect(subject.instance_variable_get(:@buffer).size).to eq 15
    end
  end

  describe 'concurrent access check' do
    context 'when flush block returns FALSE after processing' do
      it 'retains consistency if one element was pushed after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          thread = Thread.new do
            subject << 'f'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(b c d f)
      end

      it 'retains consistency if several elements were pushed after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'g'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(d f e g)
      end

      it 'retains consistency in case of empty buffer' do
        subject = SamsaraSDK::RingBuffer.new(2)
        expect(subject.empty?).to be TRUE
        subject.flush do |data|
          expect(data).to eq []
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'g'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(e g)
      end

      it 'retains consistency if one element was pushed by 2 threads each after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          threads = []
          threads << Thread.new do
            subject << 'f'
          end
          threads << Thread.new do
            subject << 'e'
          end
          threads.each(&:join)
          FALSE
        end
        expect(subject.flush).to match_array %w(c d f e)
      end

      it 'retains consistency if one element was pushed after flush to not full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        expect(subject.full?).to be FALSE
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(a b c f)
      end

      it 'retains consistency if elements were pushed after flush to not full buffer and it is still not full' do
        subject = SamsaraSDK::RingBuffer.new(7)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        expect(subject.full?).to be FALSE
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(a b c f e)
      end

      it 'retains consistency if elements were totally rewritten after flush' do
        subject = SamsaraSDK::RingBuffer.new(3)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'h'
            subject << 'o'
          end
          thread.join
          FALSE
        end
        expect(subject.flush).to eq %w(e h o)
      end
    end

    context 'when flush block returns TRUE after processing' do
      it 'retains consistency with one-element-sized buffer with pushes after flush' do
        subject = SamsaraSDK::RingBuffer.new(1)
        subject << 'a'
        subject << 'b'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(b)
          thread = Thread.new do
            subject << 'f'
            subject << 'u'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(u)
      end

      it 'retains consistency if one element was pushed after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          thread = Thread.new do
            subject << 'f'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(f)
      end

      it 'retains consistency if several elements were pushed after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'g'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(f e g)
      end

      it 'retains consistency in case of empty buffer' do
        subject = SamsaraSDK::RingBuffer.new(2)
        expect(subject.empty?).to be TRUE
        subject.flush do |data|
          expect(data).to eq []
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'g'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(e g)
      end

      it 'retains consistency if several element were pushed by 2 threads each after flush to full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject << 'd'
        expect(subject.full?).to be TRUE
        subject.flush do |data|
          expect(data).to eq %w(a b c d)
          threads = []
          threads << Thread.new do
            subject << 'f'
          end
          threads << Thread.new do
            subject << 'e'
            subject << 'h'
          end
          threads.each(&:join)
          TRUE
        end
        expect(subject.flush).to match_array %w(f e h)
      end

      it 'retains consistency if one element was pushed after flush to not full buffer' do
        subject = SamsaraSDK::RingBuffer.new(4)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        expect(subject.full?).to be FALSE
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(f)
      end

      it 'retains consistency if elements were pushed after flush to not full buffer and it is still not full' do
        subject = SamsaraSDK::RingBuffer.new(7)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        expect(subject.full?).to be FALSE
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(f e)
      end

      it 'retains consistency if elements were totally rewritten after flush' do
        subject = SamsaraSDK::RingBuffer.new(3)
        subject << 'a'
        subject << 'b'
        subject << 'c'
        subject.flush do |data|
          expect(data).to eq %w(a b c)
          thread = Thread.new do
            subject << 'f'
            subject << 'e'
            subject << 'h'
            subject << 'o'
          end
          thread.join
          TRUE
        end
        expect(subject.flush).to eq %w(e h o)
      end
    end
  end
end
