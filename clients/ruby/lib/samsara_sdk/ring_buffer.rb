# Samsara SDK
module SamsaraSDK
  # Thread-safe ring-buffer.
  class RingBuffer
    # initialize.
    #
    # @param size [Integer] Storage size.
    def initialize(size)
      @size = size
      @start = 0
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
        at = (@start + @count) % @size
        @buffer[at] = value
        if full?
          @start = (@start + 1) % @size
        else
          @count += 1
        end
        value
      end
    end
    alias <<

    # Pulls element out of buffer.
    #
    # @return [Object, nil] Element or nil if buffer is empty.
    def >>
      @mutex.synchronize do
        return nil if empty?
        value = @buffer[@start]
        @buffer[@start] = nil
        @start = (@start + 1) % @size
        @count -= 1
        value
      end
    end

    # Extract all existing elements out of buffer.
    # ??????
    # @return [Array<Object>] All buffers' elements.
    def flush
      data = snapshot
      success = block_given? ? yield(data) : TRUE
      remove data if success
      data
    end

    # Erases all stored data;
    # re-instantiates buffer.
    def clear
      @buffer = Array.new(@size)
      @start = 0
      @count = 0
    end

    def snapshot
      @buffer[pos, @buffer.size-1] + @buffer[0, pos]
      # @buffer[pos..-1] + @buffer[0...pos]
    end

    private

    # Removes element out of buffer.
    # @return [Object, nil] Element or nil if buffer is empty.
    def delete(chunk)
      @mutex.synchronize do
        @buffer = @buffer - chunk
        @count = @buffer.compact.size
        if 
      end
    end

    # Removes element out of buffer.
    #
    # @return [Object, nil] Element or nil if buffer is empty.
    # def remove
    #   return nil if empty?
    #   value = @buffer[@start]
    #   @buffer[@start] = nil
    #   @start = (@start + 1) % @size
    #   @count -= 1
    #   value
    # end
  end
end
