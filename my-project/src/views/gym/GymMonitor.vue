<template>
  <div class="monitor-container" style="padding: 24px; min-height: 100vh; background-color: #f8f9fa;">
    
    <!-- Title Section -->
    <div class="glass-header" style="margin-bottom: 24px;">
       <div class="header-left">
          <h2 style="font-weight: 300; color: #2c3e50; margin: 0;">场馆实时监控中心 <span style="font-size: 14px; color: #95a5a6; margin-left: 10px;">Live Status</span></h2>
       </div>
       <div class="legend">
          <span class="legend-item"><span class="dot available"></span> 空闲开放</span>
          <span class="legend-item"><span class="dot busy"></span> 使用中</span>
          <span class="legend-item"><span class="dot closed"></span> 关闭/维护</span>
       </div>
    </div>

    <!-- Venue Section -->
    <div style="margin-bottom: 32px;">
        <h3 class="section-label">场馆状态</h3>
        <a-row :gutter="[20, 20]">
            <a-col :span="6" v-for="venue in venues" :key="venue.id">
                <div :class="['glass-card', getStatusClass(venue.status)]">
                    <div class="card-status-line"></div>
                    <div class="card-content">
                        <div class="card-main">
                            <h3>{{ venue.name }}</h3>
                            <p class="sub-text">容量: {{ venue.capacity }}人</p>
                        </div>
                        <div class="card-icon">
                            <HomeOutlined />
                        </div>
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>

    <!-- Equipment Section -->
    <div>
        <h3 class="section-label">器材状态</h3>
        <a-row :gutter="[16, 16]">
            <a-col :span="4" v-for="eq in equipments" :key="eq.id">
                <div :class="['glass-card', 'eq-card', getEqStatusClass(eq.status)]">
                    <div class="eq-dot"></div>
                    <div class="eq-content">
                        <h4>{{ eq.name }}</h4>
                        <span class="eq-venue">{{ eq.venueId }}号厅</span>
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import request from '@/request';
import { Venue, Equipment, PageResult } from '@/types';
import { HomeOutlined } from '@ant-design/icons-vue';

const venues = ref<Venue[]>([]);
const equipments = ref<Equipment[]>([]);

const loadData = () => {
    // Load all venues
    request.get("/venue/page", { params: { pageSize: 100 } }).then((res: any) => {
        if(res.code === 200) {
            venues.value = (res.data as PageResult<Venue>).records;
        }
    });
    // Load all equipment
    request.get("/equipment/page", { params: { pageSize: 100 } }).then((res: any) => {
         if(res.code === 200) {
            equipments.value = (res.data as PageResult<Equipment>).records;
        }
    });
};

const getStatusClass = (status: number) => {
    return status === 1 ? 'status-available' : 'status-closed';
}

const getEqStatusClass = (status: string) => {
     if(status === 'AVAILABLE') return 'status-available';
    if(status === 'IN_USE') return 'status-busy';
    return 'status-closed';
}

onMounted(() => {
    loadData();
    setInterval(loadData, 30000);
});
</script>

<style scoped>
/* Modern Minimalist Glass */
.glass-header {
    background: #FDFDFD;
    backdrop-filter: blur(28px);
    -webkit-backdrop-filter: blur(8px);
    border-radius: 19px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: 0.1px solid #d9d9d9;
    box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1);
}

.section-label {
    font-size: 12px;
    font-weight: 600;
    color: #95a5a6;
    margin-bottom: 16px;
    letter-spacing: 1px;
    margin-left: 4px;
}

.glass-card {
    background: #FDFDFD;
    border-radius: 19px;
    padding: 20px;
    /* Minimal border */
    border: 0.1px solid #d9d9d9;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1);
}

.glass-card:hover {
    box-shadow: 0 8px 25px rgba(0,0,0,0.06);
    transform: translateY(-3px);
    border-color: #e6e6e6;
}

/* Venue Card Specifics */
.card-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.card-main h3 { margin: 0; font-size: 16px; font-weight: 500; color: #333; }
.sub-text { margin: 0; font-size: 13px; color: #999; margin-top: 4px; }
.card-icon { font-size: 20px; color: #eee; }

/* Equipment Card Specifics */
.eq-card { padding: 16px; display: flex; align-items: center; }
.eq-content h4 { margin: 0; font-size: 14px; font-weight: 500; color: #444; }
.eq-venue { font-size: 12px; color: #aaa; }
.eq-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 12px;
}

/* Status Colors */
.status-available .card-status-line { border-top: 3px solid #52c41a; position: absolute; top:0; left:0; right:0; }
.status-available .eq-dot { background-color: #52c41a; box-shadow: 0 0 8px rgba(82, 196, 26, 0.4); }

.status-busy .card-status-line { border-top: 3px solid #1890ff; position: absolute; top:0; left:0; right:0; }
.status-busy .eq-dot { background-color: #1890ff; box-shadow: 0 0 8px rgba(24, 144, 255, 0.4); }

.status-closed .card-status-line { border-top: 3px solid #999; position: absolute; top:0; left:0; right:0; }
.status-closed .eq-dot { background-color: #999; }

/* Legend */
.legend-item { margin-left: 20px; font-size: 13px; color: #666; font-weight: 400; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }
.dot.available { background: #52c41a; }
.dot.busy { background: #1890ff; }
.dot.closed { background: #999; }
</style>
