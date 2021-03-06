package net.minecraft.entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.EntitySenses;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet39AttachEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

public abstract class EntityLiving extends EntityLivingBase
{
    /** Number of ticks since this EntityLiving last produced its sound */
    public int livingSoundTime;

    /** The experience points the Entity gives. */
    public int experienceValue;
    private EntityLookHelper lookHelper;
    private EntityMoveHelper moveHelper;

    /** Entity jumping helper */
    private EntityJumpHelper jumpHelper;
    private EntityBodyHelper bodyHelper;
    private PathNavigate navigator;
    public final EntityAITasks tasks;
    public final EntityAITasks targetTasks;

    /** The active target the Task system uses for tracking */
    private EntityLivingBase attackTarget;
    private EntitySenses senses;

    /** Equipment (armor and held item) for this entity. */
    private ItemStack[] equipment = new ItemStack[5];

    /** Chances for each equipment piece from dropping when this entity dies. */
    protected float[] equipmentDropChances = new float[5];

    /** Whether this entity can pick up items from the ground. */
    private boolean canPickUpLoot;

    /** Whether this entity should NOT despawn. */
    private boolean persistenceRequired;
    protected float defaultPitch;

    /** This entity's current target. */
    private Entity currentTarget;

    /** How long to keep a specific target entity */
    protected int numTicksToChaseTarget;
    private boolean field_110169_bv;
    private Entity field_110168_bw;
    private NBTTagCompound field_110170_bx;

    public EntityLiving(World par1World)
    {
        super(par1World);
        this.tasks = new EntityAITasks(par1World != null && par1World.theProfiler != null ? par1World.theProfiler : null);
        this.targetTasks = new EntityAITasks(par1World != null && par1World.theProfiler != null ? par1World.theProfiler : null);
        this.lookHelper = new EntityLookHelper(this);
        this.moveHelper = new EntityMoveHelper(this);
        this.jumpHelper = new EntityJumpHelper(this);
        this.bodyHelper = new EntityBodyHelper(this);
        this.navigator = new PathNavigate(this, par1World);
        this.senses = new EntitySenses(this);

        for (int i = 0; i < this.equipmentDropChances.length; ++i)
        {
            this.equipmentDropChances[i] = 0.085F;
        }
    }

    protected void func_110147_ax()
    {
        super.func_110147_ax();
        this.func_110140_aT().func_111150_b(SharedMonsterAttributes.field_111265_b).func_111128_a(16.0D);
    }

    public EntityLookHelper getLookHelper()
    {
        return this.lookHelper;
    }

    public EntityMoveHelper getMoveHelper()
    {
        return this.moveHelper;
    }

    public EntityJumpHelper getJumpHelper()
    {
        return this.jumpHelper;
    }

    public PathNavigate getNavigator()
    {
        return this.navigator;
    }

    /**
     * returns the EntitySenses Object for the EntityLiving
     */
    public EntitySenses getEntitySenses()
    {
        return this.senses;
    }

    /**
     * Gets the active target the Task system uses for tracking
     */
    public EntityLivingBase getAttackTarget()
    {
        return this.attackTarget;
    }

    /**
     * Sets the active target the Task system uses for tracking
     */
    public void setAttackTarget(EntityLivingBase par1EntityLivingBase)
    {
        this.attackTarget = par1EntityLivingBase;
        ForgeHooks.onLivingSetAttackTarget(this, par1EntityLivingBase);
    }

    /**
     * Returns true if this entity can attack entities of the specified class.
     */
    public boolean canAttackClass(Class par1Class)
    {
        return EntityCreeper.class != par1Class && EntityGhast.class != par1Class;
    }

    /**
     * This function applies the benefits of growing back wool and faster growing up to the acting entity. (This
     * function is used in the AIEatGrass)
     */
    public void eatGrassBonus() {}

    protected void entityInit()
    {
        super.entityInit();
        this.dataWatcher.addObject(11, Byte.valueOf((byte)0));
        this.dataWatcher.addObject(10, "");
    }

    /**
     * Get number of ticks, at least during which the living entity will be silent.
     */
    public int getTalkInterval()
    {
        return 80;
    }

    /**
     * Plays living's sound at its position
     */
    public void playLivingSound()
    {
        String s = this.getLivingSound();

        if (s != null)
        {
            this.playSound(s, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    /**
     * Gets called every tick from main Entity class
     */
    public void onEntityUpdate()
    {
        super.onEntityUpdate();
        this.worldObj.theProfiler.startSection("mobBaseTick");

        if (this.isEntityAlive() && this.rand.nextInt(1000) < this.livingSoundTime++)
        {
            this.livingSoundTime = -this.getTalkInterval();
            this.playLivingSound();
        }

        this.worldObj.theProfiler.endSection();
    }

    /**
     * Get the experience points the entity currently has.
     */
    protected int getExperiencePoints(EntityPlayer par1EntityPlayer)
    {
        if (this.experienceValue > 0)
        {
            int i = this.experienceValue;
            ItemStack[] aitemstack = this.getLastActiveItems();

            for (int j = 0; j < aitemstack.length; ++j)
            {
                if (aitemstack[j] != null && this.equipmentDropChances[j] <= 1.0F)
                {
                    i += 1 + this.rand.nextInt(3);
                }
            }

            return i;
        }
        else
        {
            return this.experienceValue;
        }
    }

    /**
     * Spawns an explosion particle around the Entity's location
     */
    public void spawnExplosionParticle()
    {
        for (int i = 0; i < 20; ++i)
        {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            double d3 = 10.0D;
            this.worldObj.spawnParticle("explode", this.posX + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width - d0 * d3, this.posY + (double)(this.rand.nextFloat() * this.height) - d1 * d3, this.posZ + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width - d2 * d3, d0, d1, d2);
        }
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        super.onUpdate();

        if (!this.worldObj.isRemote)
        {
            this.func_110159_bB();
        }
    }

    protected float func_110146_f(float par1, float par2)
    {
        if (this.isAIEnabled())
        {
            this.bodyHelper.func_75664_a();
            return par2;
        }
        else
        {
            return super.func_110146_f(par1, par2);
        }
    }

    /**
     * Returns the sound this mob makes while it's alive.
     */
    protected String getLivingSound()
    {
        return null;
    }

    /**
     * Returns the item ID for the item the mob drops on death.
     */
    protected int getDropItemId()
    {
        return 0;
    }

    /**
     * Drop 0-2 items of this living's type. @param par1 - Whether this entity has recently been hit by a player. @param
     * par2 - Level of Looting used to kill this mob.
     */
    protected void dropFewItems(boolean par1, int par2)
    {
        int j = this.getDropItemId();

        if (j > 0)
        {
            int k = this.rand.nextInt(3);

            if (par2 > 0)
            {
                k += this.rand.nextInt(par2 + 1);
            }

            for (int l = 0; l < k; ++l)
            {
                this.dropItem(j, 1);
            }
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setBoolean("CanPickUpLoot", this.canPickUpLoot());
        par1NBTTagCompound.setBoolean("PersistenceRequired", this.persistenceRequired);
        NBTTagList nbttaglist = new NBTTagList();
        NBTTagCompound nbttagcompound1;

        for (int i = 0; i < this.equipment.length; ++i)
        {
            nbttagcompound1 = new NBTTagCompound();

            if (this.equipment[i] != null)
            {
                this.equipment[i].writeToNBT(nbttagcompound1);
            }

            nbttaglist.appendTag(nbttagcompound1);
        }

        par1NBTTagCompound.setTag("Equipment", nbttaglist);
        NBTTagList nbttaglist1 = new NBTTagList();

        for (int j = 0; j < this.equipmentDropChances.length; ++j)
        {
            nbttaglist1.appendTag(new NBTTagFloat(j + "", this.equipmentDropChances[j]));
        }

        par1NBTTagCompound.setTag("DropChances", nbttaglist1);
        par1NBTTagCompound.setString("CustomName", this.getCustomNameTag());
        par1NBTTagCompound.setBoolean("CustomNameVisible", this.getAlwaysRenderNameTag());
        par1NBTTagCompound.setBoolean("Leashed", this.field_110169_bv);

        if (this.field_110168_bw != null)
        {
            nbttagcompound1 = new NBTTagCompound("Leash");

            if (this.field_110168_bw instanceof EntityLivingBase)
            {
                nbttagcompound1.setLong("UUIDMost", this.field_110168_bw.func_110124_au().getMostSignificantBits());
                nbttagcompound1.setLong("UUIDLeast", this.field_110168_bw.func_110124_au().getLeastSignificantBits());
            }
            else if (this.field_110168_bw instanceof EntityHanging)
            {
                EntityHanging entityhanging = (EntityHanging)this.field_110168_bw;
                nbttagcompound1.setInteger("X", entityhanging.xPosition);
                nbttagcompound1.setInteger("Y", entityhanging.yPosition);
                nbttagcompound1.setInteger("Z", entityhanging.zPosition);
            }

            par1NBTTagCompound.setTag("Leash", nbttagcompound1);
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);
        this.setCanPickUpLoot(par1NBTTagCompound.getBoolean("CanPickUpLoot"));
        this.persistenceRequired = par1NBTTagCompound.getBoolean("PersistenceRequired");

        if (par1NBTTagCompound.hasKey("CustomName") && par1NBTTagCompound.getString("CustomName").length() > 0)
        {
            this.setCustomNameTag(par1NBTTagCompound.getString("CustomName"));
        }

        this.setAlwaysRenderNameTag(par1NBTTagCompound.getBoolean("CustomNameVisible"));
        NBTTagList nbttaglist;
        int i;

        if (par1NBTTagCompound.hasKey("Equipment"))
        {
            nbttaglist = par1NBTTagCompound.getTagList("Equipment");

            for (i = 0; i < this.equipment.length; ++i)
            {
                this.equipment[i] = ItemStack.loadItemStackFromNBT((NBTTagCompound)nbttaglist.tagAt(i));
            }
        }

        if (par1NBTTagCompound.hasKey("DropChances"))
        {
            nbttaglist = par1NBTTagCompound.getTagList("DropChances");

            for (i = 0; i < nbttaglist.tagCount(); ++i)
            {
                this.equipmentDropChances[i] = ((NBTTagFloat)nbttaglist.tagAt(i)).data;
            }
        }

        this.field_110169_bv = par1NBTTagCompound.getBoolean("Leashed");

        if (this.field_110169_bv && par1NBTTagCompound.hasKey("Leash"))
        {
            this.field_110170_bx = par1NBTTagCompound.getCompoundTag("Leash");
        }
    }

    public void setMoveForward(float par1)
    {
        this.moveForward = par1;
    }

    /**
     * set the movespeed used for the new AI system
     */
    public void setAIMoveSpeed(float par1)
    {
        super.setAIMoveSpeed(par1);
        this.setMoveForward(par1);
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        this.worldObj.theProfiler.startSection("looting");

        if (!this.worldObj.isRemote && this.canPickUpLoot() && !this.dead && this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"))
        {
            List list = this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.boundingBox.expand(1.0D, 0.0D, 1.0D));
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                EntityItem entityitem = (EntityItem)iterator.next();

                if (!entityitem.isDead && entityitem.getEntityItem() != null)
                {
                    ItemStack itemstack = entityitem.getEntityItem();
                    int i = getArmorPosition(itemstack);

                    if (i > -1)
                    {
                        boolean flag = true;
                        ItemStack itemstack1 = this.getCurrentItemOrArmor(i);

                        if (itemstack1 != null)
                        {
                            if (i == 0)
                            {
                                if (itemstack.getItem() instanceof ItemSword && !(itemstack1.getItem() instanceof ItemSword))
                                {
                                    flag = true;
                                }
                                else if (itemstack.getItem() instanceof ItemSword && itemstack1.getItem() instanceof ItemSword)
                                {
                                    ItemSword itemsword = (ItemSword)itemstack.getItem();
                                    ItemSword itemsword1 = (ItemSword)itemstack1.getItem();

                                    if (itemsword.func_82803_g() == itemsword1.func_82803_g())
                                    {
                                        flag = itemstack.getItemDamage() > itemstack1.getItemDamage() || itemstack.hasTagCompound() && !itemstack1.hasTagCompound();
                                    }
                                    else
                                    {
                                        flag = itemsword.func_82803_g() > itemsword1.func_82803_g();
                                    }
                                }
                                else
                                {
                                    flag = false;
                                }
                            }
                            else if (itemstack.getItem() instanceof ItemArmor && !(itemstack1.getItem() instanceof ItemArmor))
                            {
                                flag = true;
                            }
                            else if (itemstack.getItem() instanceof ItemArmor && itemstack1.getItem() instanceof ItemArmor)
                            {
                                ItemArmor itemarmor = (ItemArmor)itemstack.getItem();
                                ItemArmor itemarmor1 = (ItemArmor)itemstack1.getItem();

                                if (itemarmor.damageReduceAmount == itemarmor1.damageReduceAmount)
                                {
                                    flag = itemstack.getItemDamage() > itemstack1.getItemDamage() || itemstack.hasTagCompound() && !itemstack1.hasTagCompound();
                                }
                                else
                                {
                                    flag = itemarmor.damageReduceAmount > itemarmor1.damageReduceAmount;
                                }
                            }
                            else
                            {
                                flag = false;
                            }
                        }

                        if (flag)
                        {
                            if (itemstack1 != null && this.rand.nextFloat() - 0.1F < this.equipmentDropChances[i])
                            {
                                this.entityDropItem(itemstack1, 0.0F);
                            }

                            this.setCurrentItemOrArmor(i, itemstack);
                            this.equipmentDropChances[i] = 2.0F;
                            this.persistenceRequired = true;
                            this.onItemPickup(entityitem, 1);
                            entityitem.setDead();
                        }
                    }
                }
            }
        }

        this.worldObj.theProfiler.endSection();
    }

    /**
     * Returns true if the newer Entity AI code should be run
     */
    protected boolean isAIEnabled()
    {
        return false;
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    protected boolean canDespawn()
    {
        return true;
    }

    /**
     * Makes the entity despawn if requirements are reached
     */
    protected void despawnEntity()
    {
        if (this.persistenceRequired)
        {
            this.entityAge = 0;
        }
        else
        {
            EntityPlayer entityplayer = this.worldObj.getClosestPlayerToEntity(this, -1.0D);

            if (entityplayer != null)
            {
                double d0 = entityplayer.posX - this.posX;
                double d1 = entityplayer.posY - this.posY;
                double d2 = entityplayer.posZ - this.posZ;
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                if (this.canDespawn() && d3 > 16384.0D)
                {
                    this.setDead();
                }

                if (this.entityAge > 600 && this.rand.nextInt(800) == 0 && d3 > 1024.0D && this.canDespawn())
                {
                    this.setDead();
                }
                else if (d3 < 1024.0D)
                {
                    this.entityAge = 0;
                }
            }
        }
    }

    protected void updateAITasks()
    {
        ++this.entityAge;
        this.worldObj.theProfiler.startSection("checkDespawn");
        this.despawnEntity();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("sensing");
        this.senses.clearSensingCache();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("targetSelector");
        this.targetTasks.onUpdateTasks();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("goalSelector");
        this.tasks.onUpdateTasks();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("navigation");
        this.navigator.onUpdateNavigation();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("mob tick");
        this.updateAITick();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.startSection("controls");
        this.worldObj.theProfiler.startSection("move");
        this.moveHelper.onUpdateMoveHelper();
        this.worldObj.theProfiler.endStartSection("look");
        this.lookHelper.onUpdateLook();
        this.worldObj.theProfiler.endStartSection("jump");
        this.jumpHelper.doJump();
        this.worldObj.theProfiler.endSection();
        this.worldObj.theProfiler.endSection();
    }

    protected void updateEntityActionState()
    {
        super.updateEntityActionState();
        this.moveStrafing = 0.0F;
        this.moveForward = 0.0F;
        this.despawnEntity();
        float f = 8.0F;

        if (this.rand.nextFloat() < 0.02F)
        {
            EntityPlayer entityplayer = this.worldObj.getClosestPlayerToEntity(this, (double)f);

            if (entityplayer != null)
            {
                this.currentTarget = entityplayer;
                this.numTicksToChaseTarget = 10 + this.rand.nextInt(20);
            }
            else
            {
                this.randomYawVelocity = (this.rand.nextFloat() - 0.5F) * 20.0F;
            }
        }

        if (this.currentTarget != null)
        {
            this.faceEntity(this.currentTarget, 10.0F, (float)this.getVerticalFaceSpeed());

            if (this.numTicksToChaseTarget-- <= 0 || this.currentTarget.isDead || this.currentTarget.getDistanceSqToEntity(this) > (double)(f * f))
            {
                this.currentTarget = null;
            }
        }
        else
        {
            if (this.rand.nextFloat() < 0.05F)
            {
                this.randomYawVelocity = (this.rand.nextFloat() - 0.5F) * 20.0F;
            }

            this.rotationYaw += this.randomYawVelocity;
            this.rotationPitch = this.defaultPitch;
        }

        boolean flag = this.isInWater();
        boolean flag1 = this.handleLavaMovement();

        if (flag || flag1)
        {
            this.isJumping = this.rand.nextFloat() < 0.8F;
        }
    }

    /**
     * The speed it takes to move the entityliving's rotationPitch through the faceEntity method. This is only currently
     * use in wolves.
     */
    public int getVerticalFaceSpeed()
    {
        return 40;
    }

    /**
     * Changes pitch and yaw so that the entity calling the function is facing the entity provided as an argument.
     */
    public void faceEntity(Entity par1Entity, float par2, float par3)
    {
        double d0 = par1Entity.posX - this.posX;
        double d1 = par1Entity.posZ - this.posZ;
        double d2;

        if (par1Entity instanceof EntityLivingBase)
        {
            EntityLivingBase entitylivingbase = (EntityLivingBase)par1Entity;
            d2 = entitylivingbase.posY + (double)entitylivingbase.getEyeHeight() - (this.posY + (double)this.getEyeHeight());
        }
        else
        {
            d2 = (par1Entity.boundingBox.minY + par1Entity.boundingBox.maxY) / 2.0D - (this.posY + (double)this.getEyeHeight());
        }

        double d3 = (double)MathHelper.sqrt_double(d0 * d0 + d1 * d1);
        float f2 = (float)(Math.atan2(d1, d0) * 180.0D / Math.PI) - 90.0F;
        float f3 = (float)(-(Math.atan2(d2, d3) * 180.0D / Math.PI));
        this.rotationPitch = this.updateRotation(this.rotationPitch, f3, par3);
        this.rotationYaw = this.updateRotation(this.rotationYaw, f2, par2);
    }

    /**
     * Arguments: current rotation, intended rotation, max increment.
     */
    private float updateRotation(float par1, float par2, float par3)
    {
        float f3 = MathHelper.wrapAngleTo180_float(par2 - par1);

        if (f3 > par3)
        {
            f3 = par3;
        }

        if (f3 < -par3)
        {
            f3 = -par3;
        }

        return par1 + f3;
    }

    /**
     * Checks if the entity's current position is a valid location to spawn this entity.
     */
    public boolean getCanSpawnHere()
    {
        return this.worldObj.checkNoEntityCollision(this.boundingBox) && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty() && !this.worldObj.isAnyLiquid(this.boundingBox);
    }

    /**
     * Returns render size modifier
     */
    public float getRenderSizeModifier()
    {
        return 1.0F;
    }

    /**
     * Will return how many at most can spawn in a chunk at once.
     */
    public int getMaxSpawnedInChunk()
    {
        return 4;
    }

    public int func_82143_as()
    {
        if (this.getAttackTarget() == null)
        {
            return 3;
        }
        else
        {
            int i = (int)(this.func_110143_aJ() - this.func_110138_aP() * 0.33F);
            i -= (3 - this.worldObj.difficultySetting) * 4;

            if (i < 0)
            {
                i = 0;
            }

            return i + 3;
        }
    }

    /**
     * Returns the item that this EntityLiving is holding, if any.
     */
    public ItemStack getHeldItem()
    {
        return this.equipment[0];
    }

    /**
     * 0 = item, 1-n is armor
     */
    public ItemStack getCurrentItemOrArmor(int par1)
    {
        return this.equipment[par1];
    }

    public ItemStack func_130225_q(int i)
    {
        return this.equipment[i + 1];
    }

    /**
     * Sets the held item, or an armor slot. Slot 0 is held item. Slot 1-4 is armor. Params: Item, slot
     */
    public void setCurrentItemOrArmor(int par1, ItemStack par2ItemStack)
    {
        this.equipment[par1] = par2ItemStack;
    }

    public ItemStack[] getLastActiveItems()
    {
        return this.equipment;
    }

    /**
     * Drop the equipment for this entity.
     */
    protected void dropEquipment(boolean par1, int par2)
    {
        for (int j = 0; j < this.getLastActiveItems().length; ++j)
        {
            ItemStack itemstack = this.getCurrentItemOrArmor(j);
            boolean flag1 = this.equipmentDropChances[j] > 1.0F;

            if (itemstack != null && (par1 || flag1) && this.rand.nextFloat() - (float)par2 * 0.01F < this.equipmentDropChances[j])
            {
                if (!flag1 && itemstack.isItemStackDamageable())
                {
                    int k = Math.max(itemstack.getMaxDamage() - 25, 1);
                    int l = itemstack.getMaxDamage() - this.rand.nextInt(this.rand.nextInt(k) + 1);

                    if (l > k)
                    {
                        l = k;
                    }

                    if (l < 1)
                    {
                        l = 1;
                    }

                    itemstack.setItemDamage(l);
                }

                this.entityDropItem(itemstack, 0.0F);
            }
        }
    }

    /**
     * Makes entity wear random armor based on difficulty
     */
    protected void addRandomArmor()
    {
        if (this.rand.nextFloat() < 0.15F * this.worldObj.func_110746_b(this.posX, this.posY, this.posZ))
        {
            int i = this.rand.nextInt(2);
            float f = this.worldObj.difficultySetting == 3 ? 0.1F : 0.25F;

            if (this.rand.nextFloat() < 0.095F)
            {
                ++i;
            }

            if (this.rand.nextFloat() < 0.095F)
            {
                ++i;
            }

            if (this.rand.nextFloat() < 0.095F)
            {
                ++i;
            }

            for (int j = 3; j >= 0; --j)
            {
                ItemStack itemstack = this.func_130225_q(j);

                if (j < 3 && this.rand.nextFloat() < f)
                {
                    break;
                }

                if (itemstack == null)
                {
                    Item item = getArmorItemForSlot(j + 1, i);

                    if (item != null)
                    {
                        this.setCurrentItemOrArmor(j + 1, new ItemStack(item));
                    }
                }
            }
        }
    }

    public static int getArmorPosition(ItemStack par0ItemStack)
    {
        if (par0ItemStack.itemID != Block.pumpkin.blockID && par0ItemStack.itemID != Item.skull.itemID)
        {
            if (par0ItemStack.getItem() instanceof ItemArmor)
            {
                switch (((ItemArmor)par0ItemStack.getItem()).armorType)
                {
                    case 0:
                        return 4;
                    case 1:
                        return 3;
                    case 2:
                        return 2;
                    case 3:
                        return 1;
                }
            }

            return 0;
        }
        else
        {
            return 4;
        }
    }

    /**
     * Params: Armor slot, Item tier
     */
    public static Item getArmorItemForSlot(int par0, int par1)
    {
        switch (par0)
        {
            case 4:
                if (par1 == 0)
                {
                    return Item.helmetLeather;
                }
                else if (par1 == 1)
                {
                    return Item.helmetGold;
                }
                else if (par1 == 2)
                {
                    return Item.helmetChain;
                }
                else if (par1 == 3)
                {
                    return Item.helmetIron;
                }
                else if (par1 == 4)
                {
                    return Item.helmetDiamond;
                }
            case 3:
                if (par1 == 0)
                {
                    return Item.plateLeather;
                }
                else if (par1 == 1)
                {
                    return Item.plateGold;
                }
                else if (par1 == 2)
                {
                    return Item.plateChain;
                }
                else if (par1 == 3)
                {
                    return Item.plateIron;
                }
                else if (par1 == 4)
                {
                    return Item.plateDiamond;
                }
            case 2:
                if (par1 == 0)
                {
                    return Item.legsLeather;
                }
                else if (par1 == 1)
                {
                    return Item.legsGold;
                }
                else if (par1 == 2)
                {
                    return Item.legsChain;
                }
                else if (par1 == 3)
                {
                    return Item.legsIron;
                }
                else if (par1 == 4)
                {
                    return Item.legsDiamond;
                }
            case 1:
                if (par1 == 0)
                {
                    return Item.bootsLeather;
                }
                else if (par1 == 1)
                {
                    return Item.bootsGold;
                }
                else if (par1 == 2)
                {
                    return Item.bootsChain;
                }
                else if (par1 == 3)
                {
                    return Item.bootsIron;
                }
                else if (par1 == 4)
                {
                    return Item.bootsDiamond;
                }
            default:
                return null;
        }
    }

    /**
     * Enchants the entity's armor and held item based on difficulty
     */
    protected void enchantEquipment()
    {
        float f = this.worldObj.func_110746_b(this.posX, this.posY, this.posZ);

        if (this.getHeldItem() != null && this.rand.nextFloat() < 0.25F * f)
        {
            EnchantmentHelper.addRandomEnchantment(this.rand, this.getHeldItem(), (int)(5.0F + f * (float)this.rand.nextInt(18)));
        }

        for (int i = 0; i < 4; ++i)
        {
            ItemStack itemstack = this.func_130225_q(i);

            if (itemstack != null && this.rand.nextFloat() < 0.5F * f)
            {
                EnchantmentHelper.addRandomEnchantment(this.rand, itemstack, (int)(5.0F + f * (float)this.rand.nextInt(18)));
            }
        }
    }

    public EntityLivingData func_110161_a(EntityLivingData par1EntityLivingData)
    {
        this.func_110148_a(SharedMonsterAttributes.field_111265_b).func_111121_a(new AttributeModifier("Random spawn bonus", this.rand.nextGaussian() * 0.05D, 1));
        return par1EntityLivingData;
    }

    /**
     * returns true if all the conditions for steering the entity are met. For pigs, this is true if it is being ridden
     * by a player and the player is holding a carrot-on-a-stick
     */
    public boolean canBeSteered()
    {
        return false;
    }

    /**
     * Gets the username of the entity.
     */
    public String getEntityName()
    {
        return this.hasCustomNameTag() ? this.getCustomNameTag() : super.getEntityName();
    }

    public void func_110163_bv()
    {
        this.persistenceRequired = true;
    }

    public void setCustomNameTag(String par1Str)
    {
        this.dataWatcher.updateObject(10, par1Str);
    }

    public String getCustomNameTag()
    {
        return this.dataWatcher.getWatchableObjectString(10);
    }

    public boolean hasCustomNameTag()
    {
        return this.dataWatcher.getWatchableObjectString(10).length() > 0;
    }

    public void setAlwaysRenderNameTag(boolean par1)
    {
        this.dataWatcher.updateObject(11, Byte.valueOf((byte)(par1 ? 1 : 0)));
    }

    public boolean getAlwaysRenderNameTag()
    {
        return this.dataWatcher.getWatchableObjectByte(11) == 1;
    }

    @SideOnly(Side.CLIENT)
    public boolean getAlwaysRenderNameTagForRender()
    {
        return this.getAlwaysRenderNameTag();
    }

    public void setEquipmentDropChance(int par1, float par2)
    {
        this.equipmentDropChances[par1] = par2;
    }

    public boolean canPickUpLoot()
    {
        return this.canPickUpLoot;
    }

    public void setCanPickUpLoot(boolean par1)
    {
        this.canPickUpLoot = par1;
    }

    public boolean func_104002_bU()
    {
        return this.persistenceRequired;
    }

    public final boolean func_130002_c(EntityPlayer par1EntityPlayer)
    {
        if (this.func_110167_bD() && this.func_110166_bE() == par1EntityPlayer)
        {
            this.func_110160_i(true, !par1EntityPlayer.capabilities.isCreativeMode);
            return true;
        }
        else
        {
            ItemStack itemstack = par1EntityPlayer.inventory.getCurrentItem();

            if (itemstack != null && itemstack.itemID == Item.field_111214_ch.itemID && this.func_110164_bC())
            {
                if (!(this instanceof EntityTameable) || !((EntityTameable)this).isTamed())
                {
                    this.func_110162_b(par1EntityPlayer, true);
                    --itemstack.stackSize;
                    return true;
                }

                if (par1EntityPlayer.getCommandSenderName().equalsIgnoreCase(((EntityTameable)this).getOwnerName()))
                {
                    this.func_110162_b(par1EntityPlayer, true);
                    --itemstack.stackSize;
                    return true;
                }
            }

            return this.interact(par1EntityPlayer) ? true : super.func_130002_c(par1EntityPlayer);
        }
    }

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    protected boolean interact(EntityPlayer par1EntityPlayer)
    {
        return false;
    }

    protected void func_110159_bB()
    {
        if (this.field_110170_bx != null)
        {
            this.func_110165_bF();
        }

        if (this.field_110169_bv)
        {
            if (this.field_110168_bw == null || this.field_110168_bw.isDead)
            {
                this.func_110160_i(true, true);
            }
        }
    }

    public void func_110160_i(boolean par1, boolean par2)
    {
        if (this.field_110169_bv)
        {
            this.field_110169_bv = false;
            this.field_110168_bw = null;

            if (!this.worldObj.isRemote && par2)
            {
                this.dropItem(Item.field_111214_ch.itemID, 1);
            }

            if (!this.worldObj.isRemote && par1 && this.worldObj instanceof WorldServer)
            {
                ((WorldServer)this.worldObj).getEntityTracker().sendPacketToAllPlayersTrackingEntity(this, new Packet39AttachEntity(1, this, (Entity)null));
            }
        }
    }

    public boolean func_110164_bC()
    {
        return !this.func_110167_bD() && !(this instanceof IMob);
    }

    public boolean func_110167_bD()
    {
        return this.field_110169_bv;
    }

    public Entity func_110166_bE()
    {
        return this.field_110168_bw;
    }

    public void func_110162_b(Entity par1Entity, boolean par2)
    {
        this.field_110169_bv = true;
        this.field_110168_bw = par1Entity;

        if (!this.worldObj.isRemote && par2 && this.worldObj instanceof WorldServer)
        {
            ((WorldServer)this.worldObj).getEntityTracker().sendPacketToAllPlayersTrackingEntity(this, new Packet39AttachEntity(1, this, this.field_110168_bw));
        }
    }

    private void func_110165_bF()
    {
        if (this.field_110169_bv && this.field_110170_bx != null)
        {
            if (this.field_110170_bx.hasKey("UUIDMost") && this.field_110170_bx.hasKey("UUIDLeast"))
            {
                UUID uuid = new UUID(this.field_110170_bx.getLong("UUIDMost"), this.field_110170_bx.getLong("UUIDLeast"));
                List list = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(10.0D, 10.0D, 10.0D));
                Iterator iterator = list.iterator();

                while (iterator.hasNext())
                {
                    EntityLivingBase entitylivingbase = (EntityLivingBase)iterator.next();

                    if (entitylivingbase.func_110124_au().equals(uuid))
                    {
                        this.field_110168_bw = entitylivingbase;
                        break;
                    }
                }
            }
            else if (this.field_110170_bx.hasKey("X") && this.field_110170_bx.hasKey("Y") && this.field_110170_bx.hasKey("Z"))
            {
                int i = this.field_110170_bx.getInteger("X");
                int j = this.field_110170_bx.getInteger("Y");
                int k = this.field_110170_bx.getInteger("Z");
                EntityLeashKnot entityleashknot = EntityLeashKnot.func_110130_b(this.worldObj, i, j, k);

                if (entityleashknot == null)
                {
                    entityleashknot = EntityLeashKnot.func_110129_a(this.worldObj, i, j, k);
                }

                this.field_110168_bw = entityleashknot;
            }
            else
            {
                this.func_110160_i(false, true);
            }
        }

        this.field_110170_bx = null;
    }
}
