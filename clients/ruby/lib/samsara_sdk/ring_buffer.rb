# Samsara SDK
module SamsaraSDK
  # Thread-safe ring-buffer data queue tailored for Samsara Client.
  class RingBuffer
    # @return [Integer] number of pushed elements.
    attr_reader :count

    # initialize.
    #
    # @param size [Integer] Storage size.
    def initialize(size)
      @size = size
      @pointer = size - 1
      @count = 0
      @buffer = Array.new(size)
      @mutex = Mutex.new
    end

    # Is buffer full?
    #
    # @return [Boolean] Full or not.
    def full?
      @count == @size
    end

    # Is buffer empty?
    #
    # @return [Boolean] Empty or not.
    def empty?
      @count.zero?
    end

    # Puts element into buffer.
    #
    # @return [Object] Element that has been put into buffer.
    def push(value)
      @mutex.synchronize do
        @pointer = @pointer == @size - 1 ? 0 : @pointer += 1
        @buffer[@pointer] = value
        @count += 1 unless full?
        value
      end
    end
    alias << :push

    # Extract all existing elements out of buffer.
    #
    # @yield [data] Block that processed data and returns success of the processing.
    # @yieldparam [Array<Object>] data Immutable buffer snapshot. You can't modify it.
    # @yieldreturn [Boolean] Result of data processing. True if success, false otherwise.
    #
    # @return [Array<Object>] All buffer's elements.
    def flush
      data, snapshot = take_snapshot
      success = block_given? ? yield(data) : TRUE
      delete snapshot if success
      data
    end

    private

    # Get all actual elements from buffer at a given moment in FIFO order.
    # Make a snapshot of the current buffer.
    #
    # @return [Array<Object>] actual elements in FIFO order,
    # @return [Array<Object>] snapshot of current buffer.
    def take_snapshot
      @mutex.synchronize do
        i = @pointer == @size - 1 ? 0 : @pointer + 1
        return (@buffer[i..-1] + @buffer[0...i]).compact, @buffer.dup
      end
    end

    # Removes chunk of elements that present in a snapshot out of buffer.
    #
    # @param snapshot [Array<Object>] Snapshot of a buffer at some state.
    def delete(snapshot)
      @mutex.synchronize do
        @buffer.each_index { |i| @buffer[i] = nil if @buffer[i] == snapshot[i] }
        @count = @buffer.compact.size
        @pointer = @size - 1 if empty?
      end
    end
  end
end
