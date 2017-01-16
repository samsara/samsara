# Samsara SDK
module SamsaraSDK
  # Thread-safe ring-buffer data queue tailored for Samsara Client.
  class RingBuffer
    # initialize.
    #
    # @param size [Integer] Storage size.
    def initialize(size)
      @size = size
      @low = -1
      @high = -1
      @buffer = Array.new(size)
      @mutex = Mutex.new
    end

    # Get current number of items in buffer.
    #
    # @return [Integer] number of pushed elements.
    def count
      @high - @low
    end

    # Is buffer full?
    #
    # @return [Boolean] Full or not.
    def full?
      count == @size
    end

    # Is buffer empty?
    #
    # @return [Boolean] Empty or not.
    def empty?
      count.zero?
    end

    # Puts element into buffer.
    #
    # @return [Object] Element that has been put into buffer.
    def push(value)
      @mutex.synchronize do
        return if @size.zero?
        @high += 1
        @low += 1 if count > @size
        @buffer[high_position] = value
        value
      end
    end
    alias << :push

    # Extract all existing elements out of buffer.
    #
    # @yield [data] Block that processes data and returns success of the processing.
    # @yieldparam [Array<Object>] data Buffer data.
    # @yieldreturn [Boolean] Result of data processing. True if success, false otherwise.
    #
    # @return [Array<Object>] All buffer's elements in FIFO order.
    def flush
      data, at_mark = take_snapshot
      success = block_given? ? yield(data) : TRUE
      delete at_mark if success
      data
    end

    private

    # Helper-method for calculating position in a circle.
    #
    # @return [Integer] position of a given pointer.
    def calculate_position(pointer)
      @size.nonzero? ? pointer.remainder(@size) : -1
    end

    # Get array-position of the element that was last added.
    # Must be used within a synced mutex.
    #
    # @return [Integer] position.
    def high_position
      calculate_position @high
    end

    # Get array-position of the element that was last consumed.
    # Must be used within a synced mutex.
    #
    # @return [Integer] position.
    def low_position
      calculate_position @low
    end

    # Make a snapshot of the current buffer at a given moment in FIFO order.
    #
    # @return [Array<Object>] actual elements in FIFO order,
    # @return [Integer] High-water-mark that snapshot was taken at.
    def take_snapshot
      @mutex.synchronize do
        i = high_position
        j = low_position + 1

        data = if empty?
                 []
               elsif @low == -1
                 @buffer.compact
               elsif @high < @size # array is not filled completely
                 @buffer[j..i]
               else
                 @buffer[j..-1] + @buffer[0..i]
               end

        return data, @high
      end
    end

    # Removes chunk of elements that present in a snapshot out of buffer.
    # Detects if the last consumed element has been overridden by new pushes:
    # if there was overrun set low to prior to the current high;
    # otherwise set to the last consumed, denoted by the given mark.
    #
    # @param mark [Integer] high attribute that represents high-water-mark of a snapshot.
    def delete(mark)
      @mutex.synchronize do
        @low = @high - mark > @size ? @high - @size : mark
      end
    end
  end
end
