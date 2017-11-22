package net.minecraft.network.play.server;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class SPacketChunkData implements Packet<INetHandlerPlayClient>
{
    private int chunkX;
    private int chunkZ;
    private int[] availableSections;
    private byte[] buffer;
    private List<NBTTagCompound> tileEntityTags;
    private boolean loadChunk;

    public SPacketChunkData()
    {
    }

    public SPacketChunkData(Chunk p_i47124_1_, SectionMask mask)//p_i47124_2_
    {
        this.chunkX = p_i47124_1_.xPosition;
        this.chunkZ = p_i47124_1_.zPosition;
        this.loadChunk = mask.chunkload();
        boolean flag = p_i47124_1_.getWorld().provider.func_191066_m();
        this.buffer = new byte[this.calculateChunkSize(p_i47124_1_, flag, mask)];
        this.availableSections = this.extractChunkData(new PacketBuffer(this.getWriteBuffer()), p_i47124_1_, flag, mask);
        this.tileEntityTags = Lists.<NBTTagCompound>newArrayList();

        for (Entry<BlockPos, TileEntity> entry : p_i47124_1_.getTileEntityMap().entrySet())
        {
            BlockPos blockpos = (BlockPos)entry.getKey();
            TileEntity tileentity = (TileEntity)entry.getValue();
            int i = blockpos.getY() >> 4;

            if (this.doChunkLoad() || mask.getBit(i))//if (this.doChunkLoad() || (p_i47124_2_ & 1 << i) != 0)
            {
                NBTTagCompound nbttagcompound = tileentity.getUpdateTag();
                this.tileEntityTags.add(nbttagcompound);
            }
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.loadChunk = buf.readBoolean();
        this.availableSections = buf.readVarIntArray();//this.availableSections = buf.readVarIntFromBuffer();
        int i = buf.readVarIntFromBuffer();
        System.out.println("packet read mem" + i);

        if (i > 2097152)
        {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        }
        else
        {
            this.buffer = new byte[i];
            buf.readBytes(this.buffer);
            int j = buf.readVarIntFromBuffer();
            this.tileEntityTags = Lists.<NBTTagCompound>newArrayList();

            for (int k = 0; k < j; ++k)
            {
                this.tileEntityTags.add(buf.readNBTTagCompoundFromBuffer());
            }
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeInt(this.chunkX);
        buf.writeInt(this.chunkZ);
        buf.writeBoolean(this.loadChunk);
        buf.writeVarIntArray(this.availableSections);
        buf.writeVarIntToBuffer(this.buffer.length);
        buf.writeBytes(this.buffer);
        buf.writeVarIntToBuffer(this.tileEntityTags.size());

        for (NBTTagCompound nbttagcompound : this.tileEntityTags)
        {
            buf.writeNBTTagCompoundToBuffer(nbttagcompound);
        }
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(INetHandlerPlayClient handler)
    {
        handler.handleChunkData(this);
    }

    public PacketBuffer getReadBuffer()
    {
        return new PacketBuffer(Unpooled.wrappedBuffer(this.buffer));
    }

    private ByteBuf getWriteBuffer()
    {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(this.buffer);
        bytebuf.writerIndex(0);
        return bytebuf;
    }

    public int[] extractChunkData(PacketBuffer p_189555_1_, Chunk p_189555_2_, boolean p_189555_3_, SectionMask mask)
    {
        int i = 0;
        ExtendedBlockStorage[] aextendedblockstorage = p_189555_2_.getBlockStorageArray();
        int[] a = new int[aextendedblockstorage.length];
        int j = 0;

        for (int k = aextendedblockstorage.length; j < k; ++j)
        {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[j];

            if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && mask.getBit(j))//if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && (p_189555_4_.length == 1 || (p_189555_4_[j] == 1)))//if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && (p_189555_4_ & 1 << j) != 0)
            {
                i++;
                a[j] = 1;
                extendedblockstorage.getData().write(p_189555_1_);
                p_189555_1_.writeBytes(extendedblockstorage.getBlocklightArray().getData());

                if (p_189555_3_)
                {
                    p_189555_1_.writeBytes(extendedblockstorage.getSkylightArray().getData());
                }
            }
        }

        if (this.doChunkLoad())
        {
            p_189555_1_.writeBytes(p_189555_2_.getBiomeArray());
        }

        return a;
    }

    protected int calculateChunkSize(Chunk chunkIn, boolean p_189556_2_, SectionMask mask)
    {
        int i = 0;
        ExtendedBlockStorage[] aextendedblockstorage = chunkIn.getBlockStorageArray();
        int j = 0;

        for (int k = aextendedblockstorage.length; j < k; ++j)
        {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[j];

            if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && mask.getBit(j))//if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && p_189556_3_.length > 1 && (p_189556_3_[j] == 1))//if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!this.doChunkLoad() || !extendedblockstorage.isEmpty()) && (p_189556_3_ & 1 << j) != 0)
            {
                i = i + extendedblockstorage.getData().getSerializedSize();
                i = i + extendedblockstorage.getBlocklightArray().getData().length;

                if (p_189556_2_)
                {
                    i += extendedblockstorage.getSkylightArray().getData().length;
                }
            }
        }

        if (this.doChunkLoad())
        {
            i += chunkIn.getBiomeArray().length;
        }

        return i;
    }

    public int getChunkX()
    {
        return this.chunkX;
    }

    public int getChunkZ()
    {
        return this.chunkZ;
    }

    public int[] getExtractedSize()
    {
        return this.availableSections;
    }

    public boolean doChunkLoad()
    {
        return this.loadChunk;
    }

    public List<NBTTagCompound> getTileEntityTags()
    {
        return this.tileEntityTags;
    }
    
    public static class SectionMask {
    	protected long[] arr;
    	protected boolean chunkload = false;
        
        protected int bits;
        public SectionMask(int bits) {
            this.bits = bits;
            arr = new long[bits/64 + (bits%64 == 0 ? 0 : 1 )];
            this.nullify();
        }
        public SectionMask(boolean chunkload) {
        	this.chunkload = chunkload;
        }
        
        public boolean chunkload() {
        	return this.chunkload;
        }
        
        public boolean leftshift(int bits) {
            if(bits < 0) return false;
            if(bits == 0) return true;
            if((long)bits >= 64L * (long)this.arr.length) {
                this.nullify();
                return true;
            }
            
            int longcount = bits / 64;
            int rest = bits % 64;
            
            if(longcount > 0) {
                for(int i = (arr.length - longcount) - 1; i >= 0 ;i--) {
                    arr[i+longcount] = arr[i];
                }
                for(int j = longcount-1 ; j >= 0 ; j--){
                    arr[j] = 0L;
                }
            }
            
            if(rest != 0) {
                for(int i = arr.length-1; i > 0 ; i--) {
                    arr[i] = (arr[i] << rest) | (arr[i-1] >>> (64 - rest));
                }
                arr[0] = arr[0] << rest;
            }
            
            return true;
        }
        
        public boolean getBit(int pos) {
        	if(!chunkload)
        	return ((arr[pos/64] & (pos & 64)) == 0 ) ? false : true;
        	else
        		return true;
        }
        
        public void setBit(int pos,boolean value) {
        	int i = pos & 64;
        	int j = pos / 64;
        	long l = arr[j];
        	if(value) {
        		l |= 1L << i;
        	} else {
        		l &= ~(1L << i);
        	}
        }
        
        public boolean rightshift(int bits) {
            if(bits < 0) return false;
            if(bits == 0) return true;
            if((long)bits >= 64L * (long)this.arr.length) {
                this.nullify();
                return true;
            }
            
            int longcount = bits / 64;
            int rest = bits % 64;
            
            if(longcount > 0) {
                for(int i = 0 ; i < arr.length - longcount ; i++) {
                    arr[i] = arr[i+longcount];
                }
                for(int j = arr.length - longcount ; j < arr.length ; j++ ) {
                    arr[j] = 0L;
                }
            }
            System.out.println(this.toStringLines());
            if(rest != 0) {
                for(int i = 0; i < arr.length - 1 ; i++) {
                    arr[i] = (arr[i] >>> rest) | (arr[i+1] << (64-rest));
                }
                arr[arr.length-1] = arr[arr.length-1] >>> rest;
            }
            
            return true;
        }
        
        
        public void nullify() {
            for(int i = 0; i<arr.length ;i++){
                arr[i] = 0L;
            }
        }
        
        public boolean setLong(int index, long set) {
            if(index >= arr.length) return false;
            
            arr[index] = set;
            return true;
        }
        
        public String toString() {
            String s = "{";
            
            for(int i = arr.length - 1 ; i >= 0 ; i--) {
                s += bits(i);
            }
            
            return s + "}";
        }
        
        public String toStringLines() {
            String s = "{";
            
            for(int i = arr.length - 1 ; i >= 0 ; i--) {
                s += "\n" + bits(i) + " " + arr[i];
            }
            
            return s + "}";
        }
        
        public String bits(int long1) {
            String s = "[";
            
            for(int i = 63; i > 0 ;i--) {
                s += (arr[long1] & (1L << i)) == 0L ? "0," : "1,";
            }
            s += (arr[long1] & 1L) == 0L ? "0" : "1";
            
            return s + "]";
        }
    }
}
